package no.ssb.dlp.pseudo.core.func;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.Serializable;

@Data
@NoArgsConstructor @AllArgsConstructor
public class PseudoFuncRule implements Serializable {

    /**
     * Descriptive name of the rule - optional, but recommended for debug and reporting purposes
     */
    private String name;

    /**
     * Glob pattern that will trigger this rule
     */
    @NonNull
    private String pattern;

    /**
     * Pseudo function reference (including arguments such as pseudo secrets, etc) to be applied when the rule
     * is triggered.
     */
    @NonNull
    private String func;

}
