package no.ssb.dlp.pseudo.core;

import lombok.Value;
import no.ssb.dapla.dlp.pseudo.func.PseudoFunc;
import no.ssb.dlp.pseudo.core.PseudoFuncRule;

@Value
public class PseudoFuncRuleMatch {
    private final PseudoFunc func;
    private final PseudoFuncRule rule;
}
