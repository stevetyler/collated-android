!function(e){'use strict';function t(e){var t=document.createElement('style');t.innerHTML=e,document.body.appendChild(t)}function n(){t('header { display: none; }.main-container { top: .3143rem; }#refreshAppRoute { display: none; }'),document.querySelector('meta[name=viewport]').setAttribute('content','width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no')}location.href.startsWith('https://app.collated.net/')&&(e.toggleSidebar=function(){for(var e=document.getElementsByClassName('main-container'),t=0;t<e.length;t++)e[t].classList.toggle('move-right')},e.refresh=function(){document.getElementById('refreshAppRoute').click()},n())}(this.CollatedUserScript={});