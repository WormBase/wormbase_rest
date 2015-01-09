// Author: Payan Canaran <canaran@cshl.edu>
// Javascript for blast_blat page
// Copyright@2006 Cold Spring Harbor Laboratory
// $Id: search_form.js,v 1.1.1.1 2010-01-25 15:47:07 tharris Exp $

// Store dynamic options at load time
var blastAppClone          = makeCloneArray('blast_app');
var databaseClone          = makeCloneArray('database');
var versionClone           = makeCloneArray('version');
var typeClone              = makeCloneArray('typeBox');
var bioprojectClone        = makeCloneArray('bioproject');

// Other Global Vars
var queryDetermineType     = 'toggle_switch'; // OR 'sequence_entry'

function updateAllOptions() {
    updateBlastAppOptions();
    updateTypeOptions();

    // Need to update database options twice, or otherwise the BioProject
    // select box is not updated correctly.
    updateDatabaseOptions();
    updateDatabaseOptions();

    updateMessage();

    return 1;
}   

function updateBlastAppOptions() {
    var paramValues = getParamValues();
    var queryType = paramValues[0];
    var appType   = paramValues[1];
    var queryApp  = paramValues[2];

    var copy = copyArray(blastAppClone);
    var blastApp = document.getElementById('blast_app');
    updateOneOption(copy, blastApp, queryType, 'query');

    return 1;
}   
    
// function updateProcessQueryOptions() {
//     var paramValues = getParamValues();
//     var queryType = paramValues[0];
//     var appType   = paramValues[1];
//     var queryApp  = paramValues[2];
// 
//     var copy = copyArray(processQueryParamClone);
//     var processQueryParam = document.getElementById('process_query_param');
// 
//     updateOneOption(copy, processQueryParam, queryType, 'query');
// 
//     return 1;
// }   

function updateTypeOptions() {
    var paramValues = getParamValues();
    var queryType = paramValues[0];
    var version   = paramValues[3];
    var dbType    = paramValues[5];

    var copy = copyArray(typeClone);
    var typeBox = document.getElementById('typeBox');
    updateOneOption(copy, typeBox, version, 'version');
    copy = copyArray(typeBox);
    updateOneOption(copy, typeBox, dbType, 'nucl_prot');
    
    if (!typeBox.options.length) {
	var text;
	if(queryType == "prot"){ text = 'Protein'; }
	else { text = 'Genome/ESTs'; }
	var newOption = new Option(text, 'not_selected', 0, 0);
	newOption.selected = true;
        typeBox.options[0] = newOption;
    }
    
    if(typeBox.options.length == 1){ typeBox.disabled = true; }
    else { typeBox.disabled = false; }
    
    return 1;
}

function updateDatabaseOptions() {
    var paramValues = getParamValues();
    var queryType  = paramValues[0];
    var appType    = paramValues[1];
    var queryApp   = paramValues[2];
    var version    = paramValues[3];
    var type       = paramValues[4];
    var species    = paramValues[7];

    var copy = copyArray(databaseClone);
    var database = document.getElementById('database');
    updateOneOption(copy, database, version, 'version');
    copy = copyArray(database);
    updateOneOption(copy, database, type , 'type');
    copy = copyArray(database);
    updateOneOption(copy, database, queryApp, 'query-app');

    var bioprojectElement = document.getElementById('bioproject');
    copy = copyArray(bioprojectClone)
    updateOneOption(copy, bioprojectElement, species.replace(/_genome$/, ''), 'species');
    copy = copyArray(bioprojectElement)
    updateOneOption(copy, bioprojectElement, type, 'type');
    copy = copyArray(bioprojectElement)
    updateOneOption(copy, bioprojectElement, version, 'version');

    if (!database.options.length) {
        var newOption = new Option('No database available', 'not_selected', 0, 0);
        newOption.selected = true;
        
        database.options[0] = newOption;
    }

    for (var i = 0; i < bioprojectElement.options.length; i++)
        bioprojectElement.options[i].selected = true;

    return 1;
}   

function updateOneOption(cloneArray, parentElement, optionCriterion, criterion) {
     var newOptions = [];

    // Determine selectedOption
    var selectedOption;
    for (var j = 0; j < parentElement.options.length; j++) {
        if (parentElement.options[j].selected) {
           selectedOption = parentElement.options[j].value;
        }
    }    

    // If an option is already selected, make sure it's selected as well on the clone
    for (var i = 0; i < cloneArray.length; i++) {
        if (cloneArray[i].value == selectedOption) {
            cloneArray[i].selected = true;
        } else {
            cloneArray[i].selected = false;
        }    
    }
    
    // Add clone options into new options if they satisfy the criteria
    for (var i = 0; i < cloneArray.length; i++) {
        var expectedCriterion = cloneArray[i].getAttribute(criterion);

        var expectedCriterionMap = [];
        var expectedCriterionArray = expectedCriterion.split(" ");

        for (var j = 0; j < expectedCriterionArray.length; j++) {
            expectedCriterionMap[expectedCriterionArray[j]] = true;
        }    

        if (!optionCriterion 
            || expectedCriterionMap['all'] == true
            || expectedCriterionMap[optionCriterion]
            ) {
            var newOption = cloneArray[i];
            newOptions.push(newOption);
        }
    }

    // Clean options
    while (parentElement.options.length > 0) {
        parentElement.options[0] = null;
    }

    // Re-create options
    var numberOptionsSelected = 0;
    for (var i = 0; i < newOptions.length; i++) {
        parentElement.options[i] = newOptions[i];
        if (parentElement.options[i].selected) {
            numberOptionsSelected++;
        }    
    }

    // If no option is selected in the new options, select the first one
    if (!numberOptionsSelected && parentElement.options.length > 0) {
       parentElement.options[0].selected = true;
    }

    return 1;
}

function resetAllOptions() {
    var blastAppCopy = copyArray(blastAppClone);

    document.getElementById('query_sequence').value = '';
    document.getElementById('search_type_blast').checked = 1;
    document.getElementById('search_type_blat').checked = 0;
    document.getElementById('query_type_prot').checked = 1;
    document.getElementById('query_type_nucl').checked = 0;
    
    var blastApp = document.getElementById('blast_app');

    updateOneOption(blastAppCopy, blastApp, "prot", 'query');
    updateAllOptions();
    return 1;
}   

function updateMessage() {
    var userInput = document.getElementById('query_sequence').value;

    sequence = userInput.replace(/^>[^\n\r]*[\n\r]+/, '');
    sequence = sequence.replace(/[\n\r]/g,           '');
    sequence = sequence.replace(/\s+/g,              '');
    
    var messageDiv = document.getElementById("message");

    if (!sequence) {
        messageDiv.innerHTML = "Please enter a query sequence...";
        messageDiv.className = "message message-error";
    } else if (sequence.length < 10) {
        messageDiv.innerHTML = "At least 10 residues is required to perform a search!";
        messageDiv.className = "message message-error";
    } else if (!is_fasta(userInput)) {
        messageDiv.innerHTML = "Input is not in <a href=\"http://www.ncbi.nlm.nih.gov/BLAST/blastcgihelp.shtml\">FASTA format</a>...";
        messageDiv.className = "message message-error";
    } else if (document.getElementById('database').options[0].value == "not_selected") {
        messageDiv.innerHTML = "No database is available for this query-application pair!";
        messageDiv.className = "message message-error";
    } else if (document.getElementById('bioproject').selectedIndex == -1) {
        messageDiv.innerHTML = "Please select at least one BioProject...";
        messageDiv.className = "message message-error";
    } else {
        messageDiv.innerHTML = "Please click submit to perform search...";
        messageDiv.className = "message message-valid";
    }    
}    
    

function getParamValues() {
    var databaseBox   = document.getElementById('database');
    var querySequence = document.getElementById('query_sequence');
    var blastApp      = document.getElementById('blast_app');
    var versionBox    = document.getElementById('version');
    var typeBox       = document.getElementById('typeBox');
    var bioprojectBox = document.getElementById('bioproject');
    
    var species       = databaseBox.options[databaseBox.selectedIndex].value;
    species = species.split('_');
    species = species[1] + '_' + species[2];

    var version       = versionBox.options[versionBox.selectedIndex].value;
    var searchType    = typeBox.options[typeBox.selectedIndex].value;
    var bioproject    = null;

    if (bioprojectBox.selectedIndex >= 0)
        bioproject = bioprojectBox.options[bioprojectBox.selectedIndex].value;

    var queryType;
    var dbType;

    if (queryDetermineType == 'sequence_entry') {
        queryType = typeSequence(querySequence.value);
    }     
    else if (document.getElementById('query_type_nucl').checked) {
        queryType = 'nucl';
    }    
    else if (document.getElementById('query_type_prot').checked) {
        queryType = 'prot';
    }

    if (queryType == 'nucl') {
        document.getElementById('query_type_prot').checked = 0;
        document.getElementById('query_type_nucl').checked = 1;
    }    
    else {
        document.getElementById('query_type_prot').checked = 1;
        document.getElementById('query_type_nucl').checked = 0;
    }    
    
    var appType = "";
    var searchTypeBlast = document.getElementById('search_type_blast').checked;
    var searchTypeBlat  = document.getElementById('search_type_blat').checked;
    if (searchTypeBlast) {
        appType = blastApp.options[blastApp.selectedIndex].value;
	dbType = blastApp.options[blastApp.selectedIndex].getAttribute('db');
    }
    else if (searchTypeBlat) {
        appType = "blat";
	dbType = queryType;
    }
    
    var queryApp = (queryType && appType) ? queryType + ":" + appType : null;

    return new Array(queryType, appType, queryApp, version, searchType, dbType, bioproject, species);
}

function typeSequence(sequence) {
    var sequenceType = "";

    if (!sequence) {
        return sequenceType;
    }    

    sequence = sequence.replace(/^>[^\n\r]*[\n\r]+/, '');
    sequence = sequence.replace(/[\n\r]/g,           '');
    sequence = sequence.replace(/\s+/g,              '');
    sequence = sequence.toUpperCase();        

    var atcgContent = sequence.match(/[ATCG]/g);
    var atcgRatio = atcgContent ? atcgContent.length / sequence.length
                                : 0;
    
    sequenceType = atcgRatio > 0.97 ? "nucl" : "prot";

    return sequenceType;    
}
    


function makeCloneArray(id) {
    var cloneArray = [];
    var parentElement = document.getElementById(id);
    for (var i = 0; i < parentElement.options.length; i++) {
        var optionValue   = parentElement.options[i].value;
        var optionText    = parentElement.options[i].text;
        var optionQuery   = parentElement.options[i].getAttribute('query');
        var optionDb      = parentElement.options[i].getAttribute('db');
        var optionApp     = parentElement.options[i].getAttribute('query-app');
        var optionVer     = parentElement.options[i].getAttribute('version');
        var optionType    = parentElement.options[i].getAttribute('type');
        var optionNuPro   = parentElement.options[i].getAttribute('nucl_prot');
        var optionSpecies = parentElement.options[i].getAttribute('species');
	
        var newOption = new Option(optionText, optionValue, 0, 0);
        newOption.setAttribute('query',     optionQuery);
        newOption.setAttribute('db',        optionDb);
        newOption.setAttribute('query-app', optionApp);
        newOption.setAttribute('version',   optionVer);
        newOption.setAttribute('type',      optionType);	
        newOption.setAttribute('nucl_prot', optionNuPro);
        newOption.setAttribute('species',   optionSpecies);
	
        cloneArray.push(newOption);
    }

    return cloneArray;
}    

function copyArray(array) {
    var copyArray = [];
//    var parentElement = document.getElementById(id);
    for (var i = 0; i < array.length; i++) {
        var optionValue   = array[i].value;
        var optionText    = array[i].text;
        var optionQuery   = array[i].getAttribute('query');
        var optionDb      = array[i].getAttribute('db');
        var optionApp     = array[i].getAttribute('query-app');
        var optionVer     = array[i].getAttribute('version');
        var optionType    = array[i].getAttribute('type');
        var optionNuPro   = array[i].getAttribute('nucl_prot');
        var optionSpecies = array[i].getAttribute('species');
	
        var newOption = new Option(optionText, optionValue, 0, 0);
        newOption.setAttribute('query',     optionQuery);
        newOption.setAttribute('db',        optionDb);
        newOption.setAttribute('query-app', optionApp);
        newOption.setAttribute('version',   optionVer);
        newOption.setAttribute('type',      optionType);
        newOption.setAttribute('nucl_prot', optionNuPro);
        newOption.setAttribute('species',   optionSpecies);
	
        copyArray.push(newOption);
    }

    return copyArray;
}    

// Input validation. Checks whether the provided user input
// is either a raw sequence or in FASTA format. FASTA identifier
// queries are not included here.
is_fasta = function(userInput) {
    var lines = userInput.split(/[\r\n]+/);
    var state = 'init';
    var inputOkay = true;

    for (var i = 0; i < lines.length; i++) {
        if (state == 'init' || state == 'seq_or_id') {
            if (lines[i].match(/^[a-zA-Z*-][a-zA-Z *-]*\s*$/)) {
                if (state == 'init')
                    state = 'seq_only';
            } else if (lines[i].match(/^>.+$/)) {
                state = 'seq_multi_start';
            } else {
                inputOkay = false;
            }
        } else if (state == 'seq_only') {
            if (lines[i].match(/^[a-zA-Z*-][a-zA-Z *-]*\s*$/) == null) {
                inputOkay = false;
            }
        } else if (state == 'seq_multi_start') {
            if (lines[i].match(/^[a-zA-Z*-][a-zA-Z *-]*\s*$/) == null) {
                inputOkay = false;
            }
            state = 'seq_or_id';
        }
    }

    return inputOkay;
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
    }
};        

// 
// ------------------------------------------------

function addSampleNucleotide() {
    document.getElementById('query_sequence').value = sampleNucleotide;
}    

function addSamplePeptide() {
    document.getElementById('query_sequence').value = samplePeptide;
}    

DOMhelp.addEvent(document.getElementById('query_sequence'),     'change', 
                 function(){queryDetermineType = 'sequence_entry'; updateAllOptions();},   false);

DOMhelp.addEvent(document.getElementById('query_sequence'), 'keyup', updateMessage, false);

// Safari does not support onchange for radios, onclick needs to be used
DOMhelp.addEvent(document.getElementById('search_type_blast'),  'click',  updateAllOptions,    false);
DOMhelp.addEvent(document.getElementById('search_type_blat'),   'click',  updateAllOptions,    false);

DOMhelp.addEvent(document.getElementById('query_type_nucl'),    'click',
                 function(){queryDetermineType = 'toggle_switch'; updateAllOptions();},   false);
                 
DOMhelp.addEvent(document.getElementById('query_type_prot'),    'click',
                 function(){queryDetermineType = 'toggle_switch'; updateAllOptions();},   false);

DOMhelp.addEvent(document.getElementById('blast_app'),          'change', updateAllOptions,    false); 

DOMhelp.addEvent(document.getElementById('database'),          'change', updateAllOptions,    false); 

DOMhelp.addEvent(document.getElementById('version'),          'change', updateAllOptions,    false); 

DOMhelp.addEvent(document.getElementById('typeBox'),          'change', updateAllOptions,    false);

DOMhelp.addEvent(document.getElementById('bioproject'), 'change', updateMessage, false);
DOMhelp.addEvent(document.getElementById('bioproject'), 'mousedown', updateMessage, false);

// MS IE does not recognize change event on textarea if not done manually, using mouseout to supplement this
DOMhelp.addEvent(document.getElementById('sample_peptide'),     'click',
                 function(){addSamplePeptide(); queryDetermineType = 'sequence_entry'; updateAllOptions();},   false);

DOMhelp.addEvent(document.getElementById('sample_nucleotide'),  'click',
                 function(){addSampleNucleotide(); queryDetermineType = 'sequence_entry'; updateAllOptions();},   false);

DOMhelp.addEvent(document.getElementById('reset'), 'click', resetAllOptions, false);


DOMhelp.addEvent(window, 'load',  updateAllOptions, false);

////////////////////////////////////

var sampleNucleotide = '>C37F5.1                   \n\
atgaatcacattgaccttttgaaggtcaaaaaagagccgccgtcgagttc \n\
ggaagaagccgaggaagaagaatctccgaaacatacgattgagggaattt \n\
tggatataagaaagaaagagatgaacgtctcagacttgatgtgtacccga \n\
tcctcgacacctccgacctcatcatccgtcgactcaatcataaccctgtg \n\
gcaattccttctagaactactgcaacaagaccaaaatggtgatataatcg \n\
aatggacacgcggaacggacggcgaattccgactgattgatgcagaagcc \n\
gtggcgagaaagtggggacaacggaaggcgaaaccgcatatgaattatga \n\
taaactgtcgagagcgttacgatattattatgagaagaatattattaaga \n\
aggtgatcggcaaaaagttcgtatatcgctttgtaactactgacgcccac \n\
>FM864439                                          \n\
tggcatttctacgggtgatgaggtggatggagtcgccgaggaggcacact \n\
gcttgacggggagccacactatctgttggacttggtgtgctgacactgtg \n\
cgtcgtcggtgaatccgtcccaagaaggctcagcgccgagaaatccgtgt \n\
tgccacgtggattttgaggtggtggaggcggcggcggcggtgcttgtaat \n\
gacgtcataaacgacggaatctcgtgtcgaatgtccttctcgtctttgac \n\
ataacacatcttcatgttcatatttgaggaaaagtcggcggtcggcggag \n\
cgtgggcgtcagtagttacaaagcgatatacgaactttttgccgatcacc \n\
ttcttaataatattcttctcataataatatcgtaacgctctcgacagttt \n\
atcataattcatatgcggtttcgccttccgttgtccccactttctcgcca \n\
cggcttctgcatcaatcagtcggaattcgccgtccgttccgcgtgtccat \n\
tcgattatatcaccattttggtcttgttgcagtagttctagaaggaattg \n\
ccacagggttatgattgagtcgacggatgatgaggtcggaggtgtcgagg \n\
atcgggtacacatcaagtctgagacgttcatctctttctctctaatttgc \n\
gcaattaaatctatctactccttcagacattgttccgtgcccagcttctt \n\
ccgaactctacggatgctcttttttgacctaaaattgtcaatgagattca \n\
ttttcgtgtaatctgtagagggtccgcgctttgatatcctctctctactg \n\
cgtaattcatcgttactactcattcagtcatggtcaatggtcaaantttt \n\
tcncccttcttatttcctnacttccttcttctccctcacttttctttcta \n\
tctatctattcatgattaaggagcaatttattcatagcctgatactctcc \n\
ttctcccttccttatcctcctattcctccttccttcttccctttcttctt \n\
cacttagttctcgttcccctacacactctgttctttccccgtgtgccgct \n\
ggggctgttcccgctgtctgcttacgagttgtatggaccctttaacgtat \n\
ctgtgttctgggcttcacgcgagagtacttctgctgtgttcgccgcacgt \n\
attaacggatatcggcggccatgtcttgctttattctttgctaattggac \n\
tgttgctgtgttactgactccactgtcgtcgtacagacttcgacaactcc \n\
tgttatgccccatttacttgt';

var samplePeptide = '>WP:CE31440            \n\
MNHIDLLKVK KEPPSSSEEA EEEESPKHTI EGILDIRKKE \n\
MNVSDLMCTR SSTPPTSSSV DSIITLWQFL LELLQQDQNG \n\
DIIEWTRGTD GEFRLIDAEA VARKWGQRKA KPHMNYDKLS \n\
RALRYYYEKN IIKKVIGKKF VYRFVTTDAH APPTADFSSN \n\
MNMKMCYVKD EKDIRHEIPS FMTSLQAPPP PPPPPQNPRG \n\
NTDFSALSLL GTDSPTTHSV STPSPTDSVC SPSSSVASSA \n\
TPSTSSPVDE SRQCRKRSLS PSTTSSTTAP PPPPQPPTKK \n\
GMKPNPLNLT ATSNFSLQPS ISSPLLMLQQ HHQNSPLFQA \n\
QISQLYTYAA LASAGLYGPQ ISPHLASQSP FRSPLVTPKN \n\
LGLGELGSSG RTPGLGESQV FQFPPVSAFQ ATNPLLNTFS \n\
NLISPMAPFM MPPSQSSTSF KFPSSTDSLK TPTVPIKMPT \n\
L'; 

// DumperTagProperties["OPTION"] = ['text','value',];
// DumperAlert(parentElement.options);
