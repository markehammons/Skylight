package io.github.markehammons;

import wlroots.wlr_output;
import wlroots.wlr_output_h;

import java.foreign.memory.Pointer;

public class wlr_output_workaround {
    public static boolean swap_buffers(Pointer<wlr_output.wlr_output> output) {
        return wlr_output_h.wlr_output_swap_buffers(output, Pointer.ofNull(), Pointer.ofNull());
    }
}
