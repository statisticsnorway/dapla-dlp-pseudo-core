package no.ssb.dlp.pseudo.core.func;

import lombok.Value;
import no.ssb.dapla.dlp.pseudo.func.PseudoFunc;

@Value
public class PseudoFuncRuleMatch {
    private final PseudoFunc func;
    private final PseudoFuncRule rule;
}
