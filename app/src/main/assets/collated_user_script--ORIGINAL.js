// This is the original JS file from Steve Tyler. This is *not* the version of the script used in the app code, and is included here for reference.

// PLEASE NOTE - in order for the JS below to be injected successfully into the webview,
// the JS must be minified and all double quotes replaced with single quotes
// See collated_user_script.js in this folder for a working example

(function(exports) {

  "use strict";

  // Only run the user script on app.collated.net
  if (!location.href.startsWith("https://app.collated.net/")) {
    return;
  }

  exports.toggleSidebar = function() {
    var elements = document.getElementsByClassName("main-container");
    for (var i = 0; i < elements.length; i++) {
      elements[i].classList.toggle("move-right");
    }
  };

  exports.refresh = function() {
    document.getElementById("refreshAppRoute").click();
  };

  function injectStyle(style) {
    var styleElement = document.createElement("style");
    styleElement.innerHTML = style;
    document.body.appendChild(styleElement);
  }

  function setup() {
    injectStyle(
      // Completely hide the navigation bar
      "header { display: none; }" +
      // Match .main-container top spacing to .is-bookmark
      ".main-container { top: .3143rem; }" +
      // Hide refresh button
      "#refreshAppRoute { display: none; }"
    );

    // Disable zooming
    document.querySelector("meta[name=viewport]").setAttribute("content",
      "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no");
  }

  setup();

}(this.CollatedUserScript = {}));