# Wayland McWayface (JVM-edition)

Repository contains implementation of Wayland McWayface [Part 3](https://drewdevault.com/2018/02/28/Writing-a-wayland-compositor-part-3.html) by Drew DeVault. 
Jextract utility provided by project panama and `foreign` api are used directly for including bindings of wlroots and wayland within java. 
The `foreign` api can be seen in `usr.include.stdlib` in the scala sources.

## Dependencies

[build ea+44 of project panama on jdk 13](https://jdk.java.net/panama/) is required to build and run the project. 
The wayland development libraries, the wlroots library and development libraries, clang, libpixman's development libraries, libxkbcommon are also needed for the project to build and run. 
Additional libraries maybe required.

## Building

Run `sbt compile` in the root of the project directory to build the project.

Check the following settings in build.sbt if build fails:

This is an implementation of Wayland McWayface [Part 3](https://drewdevault.com/2018/02/28/Writing-a-wayland-compositor-part-3.html) by Drew DeVault. 
In order to have bindings of wlroots and wayland within java, I've made use of the jextract utility provided by project panama. 
I've also made use of the `foreign` api directly which can be seen in `usr.include.stdlib` in the scala sources.
```scala
xdgShellProtocolLocation := file("/usr/share/wayland-protocols/unstable/xdg-shell/xdg-shell-unstable-v6.xml")

includeDirectory := file("/usr/include")

libraryDirectory := file("/usr/lib64")
```

### Note for Part 3

File xdg-shell-unstable-v6-protocol.h is added from a manual build of wlroots to execute part 3. 
My distribution's wlroots-devel package does not contain this file, despite the wlr_xdg_shell_v6.h header and depending on this protocol.

## Implementation

Much of Wayland McWayface part 1 demo is written according to McWayface [repository](https://github.com/ddevault/mcwayface/blob/f89092e7d38e43c55583098beadde26b3d1235eb/src/main.c). 
Below are the list of changes:

* `wl_container_of` is not used in some places where the original demo uses it. The following implementation...
  ```c
  static void output_frame_notify(struct wl_listener *listener, void *data) {
	   struct mcw_output *output = wl_container_of(listener, output, frame);
  ```
  has changed to...
  ```scala
  lazy val output_frame_notify: FI5 = (_: Pointer[wl_listener], data: Pointer[_]) => {
  ```
  The change is necessary because scala has closures and mcw_output would have to become a C struct in order to retrieve it from the embedded listener.

* `mcw_output` does not have a `link` member because we store it in `outputs: Set[mcw_output]` within `mcw_server` instead of `wl_list`.

* `mcw_server` and `mcw_output` are implemented as constructor instead of structs.

* Some anonymous struct bindings are not comprehensible to scala because of an issue with the bytecode emitted with jextract (such as `wlroots.backend.anon$backend_h$444`). 
Java classes are created to handle the extraction of anonymous struct types from parent structs. wlr_output_workaround exists for the same reason.

  These workarounds will be removed in a future version of project panama's jdk builds 

* `wl_signal_add` is implemented in the code because it is inlined in the original library. 
* `wl_container_of` is implemented in the code because it is a macro in the original wayland library.
* There are a few api changes from the original tutorial because the wlroots api has changed between version 0.2 (which was used in Drew DeVault's tutorial) and version 0.3. This project targets wlroots version 0.3


## Notes

Part 3 shows using gnome-terminal with wayland mcwayface. I could not get gnome-terminal working, but weston-terminal works: 

![Imgur](https://i.imgur.com/1T03xi5.png)

`wl_display_terminate` is added to the `output_destroy_notify` callback close WindowManager when the display is closed.
