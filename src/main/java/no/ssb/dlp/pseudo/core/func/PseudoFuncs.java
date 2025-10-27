package no.ssb.dlp.pseudo.core.func;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.crypto.tink.*;
import no.ssb.crypto.tink.fpe.Fpe;
import no.ssb.dapla.dlp.pseudo.func.PseudoFunc;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncException;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncFactory;
import no.ssb.dapla.dlp.pseudo.func.composite.MapAndEncryptFunc;
import no.ssb.dapla.dlp.pseudo.func.composite.MapAndEncryptFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFunc;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.tink.daead.TinkDaeadFunc;
import no.ssb.dapla.dlp.pseudo.func.tink.daead.TinkDaeadFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.tink.fpe.TinkFpeFunc;
import no.ssb.dapla.dlp.pseudo.func.tink.fpe.TinkFpeFuncConfig;
import no.ssb.dlp.pseudo.core.PseudoException;
import no.ssb.dlp.pseudo.core.PseudoKeyset;
import no.ssb.dlp.pseudo.core.PseudoSecret;
import no.ssb.dlp.pseudo.core.exception.NoSuchPseudoKeyException;
import no.ssb.dlp.pseudo.core.field.FieldDescriptor;
import no.ssb.dlp.pseudo.core.tink.model.EncryptedKeysetWrapper;
import no.ssb.dlp.pseudo.core.util.Json;

import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PseudoFuncs {

    private final Map<PseudoFuncRule, PseudoFunc> ruleToFuncMap = new LinkedHashMap<>();
    private final LoadingCache<String, Aead> aeadCache;


    //TODO: Validate that all required secrets are available
    public PseudoFuncs(Collection<PseudoFuncRule> rules, Collection<PseudoSecret> pseudoSecrets,
                       Collection<PseudoKeyset> keysets, LoadingCache<String, Aead> aeadCache) {
        this.aeadCache = aeadCache;
        Map<PseudoFuncRule, PseudoFuncConfig> ruleToPseudoFuncConfigs = initPseudoFuncConfigs(rules, pseudoSecrets, keysets);
        rules.forEach(rule -> ruleToFuncMap.put(rule, PseudoFuncFactory.create(ruleToPseudoFuncConfigs.get(rule))));
    }

    // TODO: Move these init functions elsewhere?
    Map<PseudoFuncRule, PseudoFuncConfig> initPseudoFuncConfigs(Collection<PseudoFuncRule> pseudoRules,
                                                                       Collection<PseudoSecret> pseudoSecrets,
                                                                       Collection<PseudoKeyset> pseudoKeysets) {

        Map<String, PseudoSecret> pseudoSecretsMap = pseudoSecrets.stream().collect(
          Collectors.toMap(PseudoSecret::getName, Function.identity()));

        Map<String, PseudoKeyset> pseudoKeysetMap = pseudoKeysets.stream().collect(
                Collectors.toMap(PseudoKeyset::primaryKeyId, Function.identity()));

        return pseudoRules.stream().collect(Collectors.toMap(
          Function.identity(),
          rule -> {
              PseudoFuncConfig funcConfig = PseudoFuncConfigFactory.get(rule.getFunc());

              if (FpeFunc.class.getName().equals(funcConfig.getFuncImpl())) {
                  enrichLegacyFpeFuncConfig(funcConfig, pseudoSecretsMap);
              } else if (TinkDaeadFunc.class.getName().equals(funcConfig.getFuncImpl())) {
                  enrichTinkDaeadFuncConfig(funcConfig, pseudoKeysetMap, pseudoSecrets);
              } else if (TinkFpeFunc.class.getName().equals(funcConfig.getFuncImpl())) {
                  enrichTinkFpeFuncConfig(funcConfig, pseudoKeysetMap, pseudoSecrets);
              } else if (MapAndEncryptFunc.class.getName().equals(funcConfig.getFuncImpl())) {
                  // Repeat the above enrichments for MapAndEncryptFunc
                  enrichMapAndEncryptFunc(funcConfig, pseudoKeysetMap, pseudoSecretsMap, pseudoSecrets);
              }
              return funcConfig;
          }));
    }

    private void enrichMapAndEncryptFunc(PseudoFuncConfig funcConfig,
                                                Map<String, PseudoKeyset> pseudoKeysetMap,
                                                Map<String, PseudoSecret> pseudoSecretsMap,
                                                Collection<PseudoSecret> pseudoSecrets) {
        if (FpeFunc.class.getName().equals(funcConfig
                .getRequired(MapAndEncryptFuncConfig.Param.ENCRYPTION_FUNC_IMPL, String.class))) {
            enrichLegacyFpeFuncConfig(funcConfig, pseudoSecretsMap);
        } else if (TinkDaeadFunc.class.getName().equals(funcConfig
                .getRequired(MapAndEncryptFuncConfig.Param.ENCRYPTION_FUNC_IMPL, String.class))) {
            enrichTinkDaeadFuncConfig(funcConfig, pseudoKeysetMap, pseudoSecrets);
        } else if (TinkFpeFunc.class.getName().equals(funcConfig
                .getRequired(MapAndEncryptFuncConfig.Param.ENCRYPTION_FUNC_IMPL, String.class))) {
            enrichTinkFpeFuncConfig(funcConfig, pseudoKeysetMap, pseudoSecrets);
        }
    }

    private void enrichLegacyFpeFuncConfig(PseudoFuncConfig funcConfig, Map<String, PseudoSecret> pseudoSecretsMap) {
        String secretId = funcConfig.getRequired(FpeFuncConfig.Param.KEY_ID, String.class);
        if (! pseudoSecretsMap.containsKey(secretId)) {
            throw new PseudoException("No secret found for FPE pseudo func with " + FpeFuncConfig.Param.KEY_ID + "=" + secretId);
        }
        funcConfig.add(FpeFuncConfig.Param.KEY_DATA, pseudoSecretsMap.get(secretId).getBase64EncodedContent());
    }

    private void enrichTinkDaeadFuncConfig(PseudoFuncConfig funcConfig, Map<String, PseudoKeyset> keysetMap, Collection<PseudoSecret> pseudoSecrets) {
        String dekId = funcConfig.getRequired(TinkDaeadFuncConfig.Param.KEY_ID, String.class);

        // TODO: Support creating new key material instead of failing if none found?
        PseudoKeyset keyset =
                // Use keyset from provided map
                Optional.ofNullable(keysetMap.get(dekId))

                // Or search for keyset among secrets
                .or(() -> {
                    // Find secret matching key id
                    PseudoSecret secret = pseudoSecrets.stream().filter(s -> s.getId().endsWith(dekId)).findFirst().orElse(null);
                    if (secret == null) {
                        return Optional.empty();
                    }
                    EncryptedKeysetWrapper keysetWrapper = Json.toObject(EncryptedKeysetWrapper.class, new String(secret.getContent()));
                    return Optional.of(keysetWrapper);
                })
                .orElseThrow(() -> new NoSuchPseudoKeyException("No keyset with ID=" + dekId));

        try {
            String keyUri = keyset.getKekUri().toString();

            Aead masterKey = Optional.ofNullable(this.aeadCache.get(keyUri))
                    .orElseThrow(() -> new PseudoFuncException("Key material with URI " + keyUri + " not found in cache"));

            KeysetHandle keysetHandle = KeysetHandle.read(
                    JsonKeysetReader.withString(keyset.toJson()),
                    masterKey
            );

            DeterministicAead daead = keysetHandle.getPrimitive(DeterministicAead.class);
            funcConfig.add(TinkDaeadFuncConfig.Param.DAEAD, daead);
        }
        catch (Exception e) {
            throw new PseudoFuncConfigException("Error populating DaeadFuncConfig", e);
        }
    }

    private void enrichTinkFpeFuncConfig(PseudoFuncConfig funcConfig, Map<String, PseudoKeyset> keysetMap, Collection<PseudoSecret> pseudoSecrets) {
        String dekId = funcConfig.getRequired(TinkFpeFuncConfig.Param.KEY_ID, String.class);

        // TODO: Support creating new key material instead of failing if none found?
        PseudoKeyset keyset =
                // Use keyset from provided map
                Optional.ofNullable(keysetMap.get(dekId))

                        // Or search for keyset among secrets
                        .or(() -> {
                            // Find secret matching key id
                            PseudoSecret secret = pseudoSecrets.stream().filter(s -> s.getId().endsWith(dekId)).findFirst().orElse(null);
                            if (secret == null) {
                                return Optional.empty();
                            }
                            EncryptedKeysetWrapper keysetWrapper = Json.toObject(EncryptedKeysetWrapper.class, new String(secret.getContent()));
                            return Optional.of(keysetWrapper);
                        })
                        .orElseThrow(() -> new NoSuchPseudoKeyException("No keyset with ID=" + dekId));

        try {
            String keyUri = keyset.getKekUri().toString();

            Aead masterKey = Optional.ofNullable(this.aeadCache.get(keyUri))
                    .orElseThrow(() -> new PseudoFuncException("Key material with URI " + keyUri + " not found in cache"));

            KeysetHandle keysetHandle = KeysetHandle.read(
                    JsonKeysetReader.withString(keyset.toJson()),
                    masterKey
            );

            Fpe fpe = keysetHandle.getPrimitive(Fpe.class);
            funcConfig.add(TinkFpeFuncConfig.Param.FPE, fpe);
        }
        catch (Exception e) {
            throw new PseudoFuncConfigException("Error populating Tink FpeFuncConfig", e);
        }
    }


    /**
     * @return The set of pseudo rules, in the defined order
     */
    public Set<PseudoFuncRule> getRules() {
        return ruleToFuncMap.keySet();
    }

    /**
     * Inspect pseudoFuncRules (from configuration) and return an associated PseudoFunc
     * if a match is found. The first match, if any, is used. If multiple matches were found,
     * then the remaining rules are simply ignored.
     */
    public Optional<PseudoFuncRuleMatch> findPseudoFunc(FieldDescriptor field) {
        return getRules().stream()
          .filter(rule -> field.globMatches(rule.getPattern()))
          .map(rule -> new PseudoFuncRuleMatch(ruleToFuncMap.get(rule), rule))
          .findFirst();
    }

    static class PseudoFuncConfigException extends PseudoException {
        public PseudoFuncConfigException(String message, Exception e) {
            super(message, e);
        }
    }
}
