package no.ssb.dlp.pseudo.core.field;

import no.ssb.dapla.dlp.pseudo.func.PseudoFuncInput;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncOutput;
import no.ssb.dapla.dlp.pseudo.func.TransformDirection;
import no.ssb.dlp.pseudo.core.PseudoException;
import no.ssb.dlp.pseudo.core.PseudoKeyset;
import no.ssb.dlp.pseudo.core.PseudoOperation;
import no.ssb.dlp.pseudo.core.PseudoSecret;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRule;
import no.ssb.dlp.pseudo.core.func.PseudoFuncRuleMatch;
import no.ssb.dlp.pseudo.core.func.PseudoFuncs;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static no.ssb.dlp.pseudo.core.PseudoOperation.DEPSEUDONYMIZE;
import static no.ssb.dlp.pseudo.core.PseudoOperation.PSEUDONYMIZE;

@Deprecated(forRemoval = true) // Should use PseudoFuncs instead
public class FieldPseudonymizer {

    private final PseudoFuncs pseudoFuncs;

    private FieldPseudonymizer(PseudoFuncs pseudoFuncs) {
        this.pseudoFuncs = pseudoFuncs;
    }

    public PseudoFuncOutput pseudonymize(FieldDescriptor field, String varValue) {
        return process(PSEUDONYMIZE, field, varValue);
    }

    public PseudoFuncOutput depseudonymize(FieldDescriptor field, String varValue) {
        return process(DEPSEUDONYMIZE, field, varValue);
    }

    public Optional<PseudoFuncRuleMatch> match(FieldDescriptor field) {
        return pseudoFuncs.findPseudoFunc(field);
    }

    public void init(FieldDescriptor field, String varValue) {
        Optional<PseudoFuncRuleMatch> match = pseudoFuncs.findPseudoFunc(field);
        if (match.isPresent()) {
            match.get().getFunc().init(PseudoFuncInput.of(varValue), TransformDirection.APPLY);
        }
    }

    private PseudoFuncOutput process(PseudoOperation operation, FieldDescriptor field, String varValue) {

        // TODO: This check is function type specific (e.g. only applies for FPE?)
        if (varValue == null || varValue.length() <= 2) {
            return PseudoFuncOutput.of(varValue);
        }

        PseudoFuncRuleMatch match = pseudoFuncs.findPseudoFunc(field).orElse(null);
        try {
            if (match == null) {
                return PseudoFuncOutput.of(varValue);
            }

            return (operation == PSEUDONYMIZE)
              ? match.getFunc().apply(PseudoFuncInput.of(varValue))
              : match.getFunc().restore(PseudoFuncInput.of(varValue));
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
