package io.github.markehammons;

import wlroots.backend.wlr_backend;
import wlroots.backend.anon$backend_h$444;

interface WlrBackendHasExtractableEvents extends HasExtractableEvents<wlr_backend, anon$backend_h$444> {
    @Override
    default public anon$backend_h$444 extractFrom(wlr_backend backend) {
        return backend.events$get();
    }
}
