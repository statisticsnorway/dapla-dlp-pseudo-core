package no.ssb.dlp.pseudo.core;

import no.ssb.avro.convert.core.FieldDescriptor;
import no.ssb.dapla.dlp.pseudo.func.PseudoFunc;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncFactory;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFunc;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFuncConfig;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PseudoFuncs {

    private final Map<PseudoFuncRule, PseudoFunc> ruleToFuncMap = new LinkedHashMap<>();

    //TODO: Validate that all required secrets are available
    public PseudoFuncs(Collection<PseudoFuncRule> rules, Collection<PseudoSecret> pseudoSecrets) {
        Map<PseudoFuncRule, PseudoFuncConfig> ruleToPseudoFuncConfigs = initPseudoFuncConfigs(rules, pseudoSecrets);
        rules.forEach(rule -> ruleToFuncMap.put(rule, PseudoFuncFactory.create(ruleToPseudoFuncConfigs.get(rule))));
    }

    static Map<PseudoFuncRule, PseudoFuncConfig> initPseudoFuncConfigs(Collection<PseudoFuncRule> pseudoRules, Collection<PseudoSecret> pseudoSecrets) {

        Map<String, PseudoSecret> pseudoSecretsMap = pseudoSecrets.stream().collect(
          Collectors.toMap(s -> s.getName(), Function.identity()));

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

              return funcConfig;
          }));
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
