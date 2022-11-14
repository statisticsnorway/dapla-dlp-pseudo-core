package no.ssb.dlp.pseudo.core.field;

import no.ssb.dapla.dlp.pseudo.func.PseudoFuncInput;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncOutput;
import no.ssb.dlp.pseudo.core.*;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static no.ssb.dlp.pseudo.core.PseudoOperation.DEPSEUDONYMIZE;
import static no.ssb.dlp.pseudo.core.PseudoOperation.PSEUDONYMIZE;

public class FieldPseudonymizer {

    private final PseudoFuncs pseudoFuncs;

    private FieldPseudonymizer(PseudoFuncs pseudoFuncs) {
        this.pseudoFuncs = pseudoFuncs;
    }

    public String pseudonymize(FieldDescriptor field, String varValue) {
        return process(PSEUDONYMIZE, field, varValue);
    }

    public String depseudonymize(FieldDescriptor field, String varValue) {
        return process(DEPSEUDONYMIZE, field, varValue);
    }

    public Optional<PseudoFuncRuleMatch> match(FieldDescriptor field) {
        return pseudoFuncs.findPseudoFunc(field);
    }

    private String process(PseudoOperation operation, FieldDescriptor field, String varValue) {

        // TODO: This check is function type specific (e.g. only applies for FPE?)
        if (varValue == null || varValue.length() <= 2) {
            return varValue;
        }

        PseudoFuncRuleMatch match = pseudoFuncs.findPseudoFunc(field).orElse(null);
        try {
            if (match == null) {
                return varValue;
            }

            PseudoFuncOutput res = (operation == PSEUDONYMIZE)
              ? match.getFunc().apply(PseudoFuncInput.of(varValue))
              : match.getFunc().restore(PseudoFuncInput.of(varValue));
            return (String) res.getFirstValue();
        }
        catch (Exception e) {
            throw new PseudoException(operation + " error - field='" + field.getPath() + "', originalValue='" + varValue  + "'", e);
        }
    }

    public static class Builder {
        private Collection<PseudoSecret> secrets;
        private Collection<PseudoFuncRule> rules;

        private Collection<PseudoKeyset> keysets;

        public Builder secrets(Collection<PseudoSecret> secrets) {
            this.secrets = secrets;
            return this;
        }

        public Builder rules(Collection<PseudoFuncRule> rules) {
            this.rules = rules;
            return this;
        }

        public Builder keysets(Collection<PseudoKeyset> keysets) {
            this.keysets = keysets;
            return this;
        }

        public FieldPseudonymizer build() {
            Objects.requireNonNull(secrets, "PseudoSecrets can't be null");
            Objects.requireNonNull(rules, "PseudoFuncRule collection can't be null");
            return new FieldPseudonymizer(new PseudoFuncs(rules, secrets, keysets));
        }
    }
}