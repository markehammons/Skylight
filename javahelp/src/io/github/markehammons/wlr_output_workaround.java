package io.github.markehammons;

import wlroots.wlr_output_h;
import wlroots.wlr_output_lib;

import java.foreign.memory.Pointer;

public class wlr_output_workaround {
    public static boolean swap_buffers(Pointer<wlr_output_h.wlr_output> output) {
        return wlr_output_lib.wlr_output_commit(output);
    }
}
