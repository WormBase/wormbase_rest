var geneID;
var updateSource;

function wb_gene_init() {
    var match = /gene\/(.+)/.exec(window.location.pathname);
    if (match) {
        geneID = match[1];
    }

    updateSource = new EventSource('/updates');
    initWidget('Overview', '/gene-overview/');
    initWidget('Phenotypes', '/gene-phenotypes/');
    initWidget('References', '/gene-refs/');
}

function initWidget(name, prefix) {
    var contentHolder = makeElement('div', [
        "Hello"
    ], {className: 'content'});
    
    var widget = makeElement('div', [
        makeElement('div', [
            makeElement('div', null, {className: 'module-close ui-icon ui-icon-large ui-icon-close'}),
            makeElement('h3', [
                makeElement('div', null, {className: 'module-min ui-icon-large ui-icon-triangle-1-s'}),
                makeElement('span', name, {className: 'widget-title'}),
                makeElement('span', null, {className: 'ui-icon ui-icon-arrow-4 hide ui-helper-hidden'})])],
                    {className: 'ui-corner-top widget-header'}),
        contentHolder,
        makeElement('div', null, {id: 'widget-footer', className: 'ui-helper-hidden'}),
        makeElement('div', null, {id: 'widget-feed'})], {className: 'widget-container ui-corner-all'},
                             {marginTop: '50px'});


    document.getElementById('widget-holder').appendChild(makeElement('li', widget, {className: "widget visible"}));

    var refresh = function() {
        xhr(prefix + geneID)
            .then(function(resp) {
                contentHolder.innerHTML = resp;
            })
            .catch(function(err) {
                console.log(err);
            });
    };
    refresh();
    updateSource.addEventListener('message', function(ev) {
        var msg = JSON.parse(ev.data);
        if (msg.type == 'gene' && msg.id == geneID && name == "Overview") {
            refresh();
        }
    });
}


function xhr(uri) {
    return new Promise(function(resolve, reject) {
        var req = new XMLHttpRequest();
        req.onreadystatechange = function() {
            if (req.readyState == 4) {
                if (req.status >= 300) {
                    return reject('Error ' + req.status);
                } else {
                    return resolve(req.responseText);
                }
            }
        }
        req.open('GET', uri, true);
        req.responseType = 'text';
        req.send('');
    });
}

function makeElement(tag, children, attribs, styles)
{
    var ele = document.createElement(tag);
    if (children) {
        if (! (children instanceof Array)) {
            children = [children];
        }
        for (var i = 0; i < children.length; ++i) {
            var c = children[i];
            if (c) {
                if (typeof c == 'string') {
                    c = document.createTextNode(c);
                } else if (typeof c == 'number') {
                    c = document.createTextNode('' + c);
                }
                ele.appendChild(c);
            }
        }
    }
    
    if (attribs) {
        for (var l in attribs) {
            try {
                ele[l] = attribs[l];
            } catch (e) {
                console.log('error setting ' + l);
                throw(e);
            }
        }
    }
    if (styles) {
        for (var l in styles) {
            ele.style[l] = styles[l];
        }
    }
    return ele;
}
