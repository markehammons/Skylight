# Wayland McWayface (JVM-edition)

This is an implementation of Wayland McWayface [Part 3](https://drewdevault.com/2018/02/28/Writing-a-wayland-compositor-part-3.html) by Drew DeVault. 
In order to have bindings of wlroots and wayland within java, I've made use of the jextract utility provided by project panama. 
However, I've also made use of the `foreign` api directly as well, which can be seen in `usr.include.stdlib` in the scala sources.

## Dependencies

In order to build and run this project you will need [a build of project panama on jdk 13](https://jdk.java.net/panama/). 
You'll also need the wayland development libraries, the wlroots library and development libraries, clang, libpixman's development libraries, and possibly more.

## Building

In order to build this project, you must first run the following command in the root of this project:

```bash
~/bin/jdk-13/bin/jextract /usr/include/wlr/types/wlr_output.h \
 /usr/include/wlr/backend.h \
 /usr/include/wlr/render/wlr_renderer.h \
 /usr/include/wlr/types/wlr_idle.h \
 /usr/include/wlr/types/wlr_gamma_control.h \
 /usr/include/wlr/types/wlr_screenshooter.h \
 /usr/include/wlr/types/wlr_primary_selection_v1.h \
 /usr/include/wlr/types/wlr_xdg_shell_v6.h \ 
 /usr/include/wlr/types/wlr_surface.h \
 /usr/include/wlr/types/wlr_box.h \
 /usr/include/wlr/types/wlr_matrix.h \
 -m /usr/include/wlr/backend=wlroots.backend_headers \
 -m /usr/include/bits/types=usr.include.bits.type_headers \
 -I /usr/include/wlr \
 -I /usr/include/wayland \
 -I /usr/include/pixman-1 \
 -I /usr/include/libxkbcommon \
 -I include/
 -C "-DWLR_USE_UNSTABLE" \
 -L /usr/lib64 \
 --record-library-path \
 -l wlroots \
 -t wlroots \
 -o lib/wlroots.jar
```

Then, you can run `sbt run` to activate the demo.

Please note that this command may change depending on your linux distribution. 
The important part to remember here is that you need to point to the header locations for wlr, wayland,
pixman, and libxkbcommon with the -I flags, point to the location of the `.so` files with the -L command,
and specify the specific headers you want to depend on like the first arguments to jextract above.

### Note for Part 3
I had to add the file xdg-shell-unstable-v6-protocol.h from a manual build of wlroots to get this step able to work.
My distribution's wlroots-devel package does not contain this file, despite the wlr_xdg_shell_v6.h header and depending
on this protocol

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