package io.github.markehammons

import java.foreign.{NativeTypes, Scope}

import org.scalatest.{DiagrammedAssertions, WordSpec}
import usr.include.wayland.wayland_server_h.wl_resource
import usr.include.wayland.wayland_util_h.wl_list
import usr.include.wayland.wayland_util_lib.{wl_list_init, wl_list_insert}

class mainSpec extends WordSpec {
//  "wl_container_of" should {
//    "extract wl_resource from wl_list" in {
//      val testScope = Scope.globalScope().fork()
//
//      val wl_list = testScope.allocateStruct(classOf[wl_list])
//
//      val wl_resource = testScope.allocateStruct(classOf[wl_resource])
//
//      val testData = testScope.allocateArray(NativeTypes.INT, Array(1,2,3,4))
//
//      wl_resource.data$set(testData.elementPointer().cast(NativeTypes.VOID))
//
//      val r = utils.wl_container_of[wl_resource](wl_resource.link$ptr())
//
//      assert(r.get().data$get().addr() == testData.elementPointer().addr())
//
//      testScope.close()
//    }
//  }

}
