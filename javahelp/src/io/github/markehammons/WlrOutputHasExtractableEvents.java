package io.github.markehammons;

import wlroots.wlr_output_h;
import wlroots.wlr_output_h.wlr_output;

public interface WlrOutputHasExtractableEvents extends HasExtractableEvents<wlr_output, wlr_output_h.anon$wlr_output_h$2608> {
    default wlr_output_h.anon$wlr_output_h$2608 extractFrom(wlr_output output) {
        return output.events$get();
    }
}
