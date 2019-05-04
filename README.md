# Skylight

This is an implementation of Wayland McWayface [Part 3](https://drewdevault.com/2018/02/28/Writing-a-wayland-compositor-part-3.html) by Drew DeVault. 
In order to have bindings of wlroots and wayland within java, I've made use of the jextract utility provided by project panama. 
However, I've also made use of the `foreign` api directly as well, which can be seen in `usr.include.stdlib` in the scala sources.

In the future, this project will become it's own thing, taking some cues from tinywm by the wlroots project.

## Dependencies

In order to build and run this project you will need [build ea+70 of project panama on jdk 13](https://jdk.java.net/panama/). 
You'll also need the wayland development libraries, the wlroots library and development libraries, clang, libpixman's development libraries, libxkbcommon and possibly others. 

## Building

Run `sbt compile` in the root of the project directory to build the project.

Check the following settings in build.sbt if build fails:

```scala
xdgShellProtocolLocation := file("/usr/share/wayland-protocols/unstable/xdg-shell/xdg-shell-unstable-v6.xml")

includeDirectory := file("/usr/include")

libraryDirectory := file("/usr/lib64")
```

Make sure they point to your xdg-shell-unstable-v6.xml, your include path, and the path that contains the .so files on your system.


## Implementation

I have implemented the Wayland McWayface part 3 demo as written [here](https://github.com/ddevault/mcwayface/blob/f89092e7d38e43c55583098beadde26b3d1235eb/src/main.c) for the most part. Below are the list of changes I've made...

* `wl_container_of` is not used in some places where the original demo uses it. The following implementation...
  ```c
  static void output_frame_notify(struct wl_listener *listener, void *data) {
	   struct mcw_output *output = wl_container_of(listener, output, frame);
  ```
  has changed to...
  ```scala
  lazy val output_frame_notify: FI5 = (_: Pointer[wl_listener], data: Pointer[_]) => {
  ```
  I've made this change because scala has closures and mcw_output would have to become a C struct in order to retrieve it from the listener embedded within it.

* `mcw_output` does not have a `link` member because we store it in `outputs: Set[mcw_output]` within `mcw_server` instead a `wl_list`.

* I use constructors for `mcw_server` and `mcw_output` instead of treating them as structs.
* I've implemented `wl_signal_add` in my code because it is inlined in the original library. 
* I've implemented `wl_container_of` in my code because it's a macro in the original wayland library.
* There are a few api changes from the original tutorial because the wlroots api has changed between version 0.2 (which was used in Drew DeVault's tutorial) and version 0.3. This project targets wlroots version 0.3


## Notes

In part 3 it shows using gnome-terminal with wayland mcwayface. I could not get gnome-terminal working, but weston-terminal works just fine: 

![Imgur](https://i.imgur.com/1T03xi5.png)

Also, I've added `wl_display_terminate` to the `output_destroy_notify` callback in order to have the WindowManager close when the display is closed.
