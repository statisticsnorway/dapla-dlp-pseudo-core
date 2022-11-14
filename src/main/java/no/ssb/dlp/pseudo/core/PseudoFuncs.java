package no.ssb.dlp.pseudo.core;

import com.google.crypto.tink.DeterministicAead;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.KmsClients;
import no.ssb.dapla.dlp.pseudo.func.PseudoFunc;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncFactory;
import no.ssb.dapla.dlp.pseudo.func.daead.DaeadFunc;
import no.ssb.dapla.dlp.pseudo.func.daead.DaeadFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFunc;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFuncConfig;
import no.ssb.dlp.pseudo.core.field.FieldDescriptor;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PseudoFuncs {

    private final Map<PseudoFuncRule, PseudoFunc> ruleToFuncMap = new LinkedHashMap<>();

    //TODO: Validate that all required secrets are available
    public PseudoFuncs(Collection<PseudoFuncRule> rules, Collection<PseudoSecret> pseudoSecrets, Collection<PseudoKeyset> keysets) {
        Map<PseudoFuncRule, PseudoFuncConfig> ruleToPseudoFuncConfigs = initPseudoFuncConfigs(rules, pseudoSecrets, keysets);
        rules.forEach(rule -> ruleToFuncMap.put(rule, PseudoFuncFactory.create(ruleToPseudoFuncConfigs.get(rule))));
    }

    // TODO: Move these init functions elsewhere?
    static Map<PseudoFuncRule, PseudoFuncConfig> initPseudoFuncConfigs(Collection<PseudoFuncRule> pseudoRules, Collection<PseudoSecret> pseudoSecrets, Collection<PseudoKeyset> pseudoKeysets) {

        Map<String, PseudoSecret> pseudoSecretsMap = pseudoSecrets.stream().collect(
          Collectors.toMap(PseudoSecret::getName, Function.identity()));

        Map<String, PseudoKeyset> pseudoKeysetMap = pseudoKeysets.stream().collect(
                Collectors.toMap(PseudoKeyset::getPrimaryKeyId, Function.identity()));

        return pseudoRules.stream().collect(Collectors.toMap(
          Function.identity(),
          rule -> {
              PseudoFuncConfig funcConfig = PseudoFuncConfigFactory.get(rule.getFunc());

              if (FpeFunc.class.getName().equals(funcConfig.getFuncImpl())) {
                  String secretId = funcConfig.getRequired(FpeFuncConfig.Param.KEY_ID, String.class);
                  if (! pseudoSecretsMap.containsKey(secretId)) {
                      throw new PseudoException("No secret found for FPE pseudo func with " + FpeFuncConfig.Param.KEY_ID + "=" + secretId);
                  }
                  funcConfig.add(FpeFuncConfig.Param.KEY, pseudoSecretsMap.get(secretId).getBase64EncodedContent());
              }

              else if (DaeadFunc.class.getName().equals(funcConfig.getFuncImpl())) {
                  enrichDaeadFuncConfig(funcConfig, pseudoKeysetMap);
              }

              return funcConfig;
          }));
    }

    private static void enrichDaeadFuncConfig(PseudoFuncConfig funcConfig, Map<String, PseudoKeyset> keysetMap) {
        String dekId = funcConfig.getRequired(DaeadFuncConfig.Param.DEK_ID, String.class);

        // TODO: Support creating new key material instead of failing
        PseudoKeyset keyset = Optional.ofNullable(keysetMap.get(dekId))
                .orElseThrow(() -> new RuntimeException("No keyset with ID=" + dekId));

        try {

            KeysetHandle keysetHandle = KeysetHandle.read(
                    JsonKeysetReader.withString(keyset.toJson()),
                    KmsClients.get(keyset.getKekUri()).getAead(keyset.getKekUri())
            );

            DeterministicAead daead = keysetHandle.getPrimitive(DeterministicAead.class);
            funcConfig.add(DaeadFuncConfig.Param.DAEAD, daead);
        }
        catch (Exception e) {
            throw new RuntimeException("Error populating DaeadFuncConfig", e);
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

}
