package io.github.markehammons

import wlroots.wlr_output_h.wlr_output

given wlrops {
  def (x: wlr_output) id: wlr_output = x
}