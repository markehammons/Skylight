# Wayland McWayface (JVM-edition)

This is an implementation of Wayland McWayface [Part 1](https://drewdevault.com/2018/02/17/Writing-a-Wayland-compositor-1.html) by Drew DeVault. 
In order to have bindings of wlroots and wayland within java, I've made use of the jextract utility provided by project panama.

## Dependencies

In order to build and run this project you will need [a build of project panama on jdk 13](https://jdk.java.net/panama/). 
You'll also need the wayland development libraries, the wlroots library and development libraries, clang, libpixman's development libraries, and possibly more.

## Building

In order to build this project, you must first run the following command in the root of this project:

`jextract /usr/include/wlr/types/wlr_output.h /usr/include/wlr/backend.h /usr/include/wlr/render/wlr_renderer.h -m /usr/include/wlr/backend=wlroots.backend_headers -m /usr/include/bits/types=usr.include.bits.type_headers -I /usr/include/wlr -I /usr/include/wayland -I /usr/include/pixman-1 -C "-DWLR_USE_UNSTABLE" -L /usr/lib64 --record-library-path -l wlroots -t wlroots -o lib/wlroots.jar`

Then, you can run `sbt run` to activate the demo.

## Implementation

I have implemented the Wayland McWayface part 1 demo as written [here](https://github.com/ddevault/mcwayface/blob/f89092e7d38e43c55583098beadde26b3d1235eb/src/main.c) for the most part. Below are the list of changes I've made...


* I did not use `wl_container_of` in a few places where the original demo did. Instead of 
  ```c
  static void output_frame_notify(struct wl_listener *listener, void *data) {
	   struct mcw_output *output = wl_container_of(listener, output, frame);
  ```
  I've written
  ```scala
  def output_frame_notify(output: mcw_output): FI5 = (_: Pointer[wl_listener], data: Pointer[_]) => {
  ```
  because scala has closures and mcw_output would have to become a C struct in order to retrieve it from the listener embedded in it.

* `mcw_output` does not have a `link` member cause we store it in `outputs: Set[mcw_output]` within `mcw_server` instead a `wl_list`.

* I use constructors for `mcw_server` and `mcw_output` instead of treating them as structs.

* Because of an issue with the bytecode emitted with jextract, some anonymous struct bindings are not comprehensible to scala
(such as `wlroots.backend.anon$backend_h$444`). Because of this I've created java classes to handle the extraction of these 
anonymous struct types from their parent structs. wlr_output_workaround exists for much the same reason.

  I should be able to remove these workarounds in a future version of project panama's jdk builds 

* I've implemented `wl_signal_add` in my code because it is inlined in the original library. 
* I've implemented `wl_container_of` in my code because it's a macro in the original wayland library.