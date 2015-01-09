// -------------------------------------------------------------------
// markItUp!
// -------------------------------------------------------------------
// Copyright (C) 2008 Jay Salvat
// http://markitup.jaysalvat.com/
// -------------------------------------------------------------------
// Mediawiki Wiki tags example
// -------------------------------------------------------------------
// Feel free to add more tags
// -------------------------------------------------------------------
mySettings = {
    previewParserPath:  '', // path to your Wiki parser
    onShiftEnter:       {keepDefault:false, replaceWith:'\n\n'},
    markupSet: [
        {name:'Heading 1', key:'1', openWith:'## ', closeWith:"\n", placeHolder:'Your title here...' },
        {name:'Heading 2', key:'2', openWith:'### ', closeWith:"\n", placeHolder:'Your title here...' },
        {name:'Heading 3', key:'3', openWith:'#### ', closeWith:"\n", placeHolder:'Your title here...' },
        {name:'Heading 4', key:'4', openWith:'##### ', closeWith:"\n", placeHolder:'Your title here...' },
        {name:'Heading 5', key:'5', openWith:'###### ', closeWith:"\n", placeHolder:'Your title here...' },
        {separator:'---------------' },     
        {name:'Bold', key:'B', openWith:"**", closeWith:"**"}, 
        {name:'Italic', key:'I', openWith:"*", closeWith:"*"}, 
        {name:'Stroke through', key:'S', openWith:'<s>', closeWith:'</s>'}, 
        {separator:'---------------' },
        {name:'Bulleted list', openWith:'(!(* |!|*)!)'}, 
        {name:'Numeric list', openWith:'(!(1. |!|1.)!)'}, 
        {separator:'---------------' },
        {name:'Picture', key:"P", replaceWith:'![[![alt text]!]]([![Url:!:http://]!])'}, 
        {name:'Link', key:"L", openWith:"[ ", closeWith:' ]([![Url:!:http://]!]) ', placeHolder:'Your text to link here...' },
        {separator:'---------------' },
        {name:'Quotes', openWith:'>', placeHolder:''},
        {name:'Code', openWith:'`', closeWith:'`'}, 
        {name:'Line', openWith:'***'}, 
    ]
}