package io.github.markehammons;

import wlroots.wlr_output;

public interface WlrOutputHasExtractableEvents extends HasExtractableEvents<wlr_output.wlr_output, wlr_output.anon$wlr_output_h$2137> {
    default wlr_output.anon$wlr_output_h$2137 extractFrom(wlr_output.wlr_output output) {
        return output.events$get();
    }
}
