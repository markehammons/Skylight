package org.swaywm.wlroots

import java.foreign.memory.Pointer
import wlroots.wlr_output_h.{wlr_output,wlr_output_mode}
import wlroots.wlr_output_lib.{wlr_output_set_mode, wlr_output_create_global, wlr_output_attach_render, wlr_output_commit}
import usr.include.wayland.wayland_util_h.wl_list
import org.freedesktop.wayland.WlList

type WlrOutputP = Pointer[wlr_output]

object WlrOutputP
  given: (output: WlrOutputP)
    inline def setMode(mode: Pointer[wlr_output_mode]): Unit =
      wlr_output_set_mode(output, mode)
    
    inline def createGlobal(): Unit = 
      wlr_output_create_global(output)

    inline def attachRender(render: Pointer[Integer]): Unit =
      wlr_output_attach_render(output, render)

    inline def commit(): Unit =
      wlr_output_commit(output)

type WlrOutput = wlr_output

object WlrOutput 
  given: (output: WlrOutput)
      inline def width: Int = output.width$get
      //inline def width_=(value: Int): Unit = output.width$set(value)
      inline def height: Int = output.height$get

      inline def modes: WlList[wlr_output_mode] = WlList[wlr_output_mode](output.modes$ptr)