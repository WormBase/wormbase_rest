// Author: Payan Canaran <canaran@cshl.edu>
// Javascript for blast_blat page
// Copyright@2006 Cold Spring Harbor Laboratory
// $Id: display_results.js,v 1.1.1.1 2010-01-25 15:47:07 tharris Exp $

function attachExpandEvents() {
    var expandButtons = document.getElementsByTagName('img');

    for (var i = 0; i < expandButtons.length; i++) {

         var elementClass = expandButtons[i].getAttribute('class') || expandButtons[i].getAttribute('className'); // OR expandButtons[i].className for MSIE
         
         if (elementClass != 'expand_button') { 
             continue;
         }
        
        var elementExpandLink      = expandButtons[i].getAttribute('expand_link');
        var elementExpandAreaCount = expandButtons[i].getAttribute('expand_area_count');

        expandButtons[i].setAttribute('src', '/img/blast_blat/plus.png');

        DOMhelp.addEvent(expandButtons[i], 'click',  toggleExpandArea, false);
    }
}

function toggleExpandArea(e) {
    var eventTarget           = DOMhelp.getTarget(e);
    var targetExpandAreaCount = eventTarget.getAttribute('expand_area_count');
    var targetSrc             = eventTarget.getAttribute('src');

    var expandAreas = document.getElementsByTagName('div');
    for (var i = 0; i < expandAreas.length; i++) {
        var elementClass = expandAreas[i].getAttribute('class') || expandAreas[i].getAttribute('className'); // OR expandAreas[i].className for MSIE

        if (elementClass != 'expand_area') { 
            continue;
        }

        var elementExpandAreaCount = expandAreas[i].getAttribute('expand_area_count');

        if (elementExpandAreaCount != targetExpandAreaCount) {
            continue;
        }
            
        var elementExpandLink      = expandAreas[i].getAttribute('expand_link');

        if (/\/plus.png$/.test(targetSrc)) {
            eventTarget.setAttribute('src', '/img/blast_blat/minus.png');
            expandAreas[i].innerHTML = '<img src="' + elementExpandLink + '" />';
        }
        else if (/\/minus.png$/.test(targetSrc)) {
            eventTarget.setAttribute('src', '/img/blast_blat/plus.png');
            expandAreas[i].innerHTML = '';
        }
    }
}

// ------------------------------------------------
// 
//     addEvent function is an excerpt from:
//     
// 	DOMhelp 1.0
// 	written by Chris Heilmann
// 	http://www.wait-till-i.com
// 	To be featured in "Beginning JavaScript for Practical Web Development, Including  AJAX" 
// 

DOMhelp={
	addEvent: function(elm, evType, fn, useCapture){
		if (elm.addEventListener){
			elm.addEventListener(evType, fn, useCapture);
			return true;
		} else if (elm.attachEvent) {
			var r = elm.attachEvent('on' + evType, fn);
			return r;
		} else {
			elm['on' + evType] = fn;
		}
    },
  	getTarget:function(e){
    	var target = window.event ? window.event.srcElement : e ? e.target : null;
		if (!target){return false;}
		while(target.nodeType!=1 && target.nodeName.toLowerCase()!='body'){
			target=target.parentNode;
		}
	return target;
	}
};        

// 
// ------------------------------------------------

function debug(message) {
    document.getElementById("message2").innerHTML += message + "<br>";    
}    

DOMhelp.addEvent(window, 'load',  attachExpandEvents, false);
