
// CouchDB: URL and report database
var couchBaseUri = 'http://dev.wormbase.org:5984';
var couchReportDB = 'reports';
var couchBrowsePath = '_utils/document.html?' + couchReportDB;

// Number of GBrowse detailed view columns ("#gbrowsedetails0", "#gbrowsedetails1", ...):
var gbrowseDetailsColumns = 2;

var gbrowseOverviewGraph = null;
var gbrowseOverviewData = {
  totalUrls: [],
  brokenUrls: [],
  imageMismatches: [],
  missingReferences: [],
  tracks: [],
  landmarks: []
};

var gbrowseDetailGraphs = {};
var gbrowseDetailData = {};

var epoch2URL = {};

function couchRequest(request, callback) {
  jQuery.ajax({
    type: 'GET',
    url: couchBaseUri + "/" + couchReportDB + "/" + request,
    processData: true,
    data: {},
    dataType: "json",
    success: function(data) {
      callback(data, null);
    },
    error: function(request, textStatus) {
      callback(null, textStatus);
    }
  });
}

function configuration2Title(configuration) {
  var chunks = configuration.split("_");

  return chunks[0].toUpperCase() + ". " + chunks[1] + " (BioProject " + chunks[2] + ")";
}

function getReportListing(callback) {
  couchRequest("_all_docs", callback);
}

function getReportOverview(data, error) {
  if (error) {
     // TODO
     return;
  }

  if (data["type"] != "gbrowse" || !data["completed"])
    return;

  var totalUrls = 0;
  var brokenUrls = 0;
  var imageMismatches = 0;
  var missingReferences = 0;
  var tracks = 0;
  var landmarks = 0;
  for (var i = 0; i < data["configurations"].length; i++) {
    totalUrls += data[data["configurations"][i]]["total_urls"];
    brokenUrls += data[data["configurations"][i]]["broken_urls"];
    imageMismatches += data[data["configurations"][i]]["image_mismatches"];
    missingReferences += data[data["configurations"][i]]["missing_references"];
    tracks += data[data["configurations"][i]]["tracks"];
    landmarks += data[data["configurations"][i]]["example_landmarks"];

    updateReportDetails(data["configurations"][i], i % gbrowseDetailsColumns, {
      "_id": data["_id"],
      "started_since_epoch": data["started_since_epoch"],
      "totalUrls": data[data["configurations"][i]]["total_urls"],
      "brokenUrls": data[data["configurations"][i]]["broken_urls"],
      "imageMismatches": data[data["configurations"][i]]["image_mismatches"],
      "missingReferences": data[data["configurations"][i]]["missing_references"],
      "tracks": data[data["configurations"][i]]["tracks"],
      "landmarks": data[data["configurations"][i]]["example_landmarks"]
    });
  }
  gbrowseOverviewData["totalUrls"].push([ data["started_since_epoch"] * 1000, totalUrls ]);
  gbrowseOverviewData["brokenUrls"].push([ data["started_since_epoch"] * 1000, brokenUrls ]);
  gbrowseOverviewData["imageMismatches"].push([ data["started_since_epoch"] * 1000, imageMismatches ]);
  gbrowseOverviewData["missingReferences"].push([ data["started_since_epoch"] * 1000, missingReferences ]);
  gbrowseOverviewData["tracks"].push([ data["started_since_epoch"] * 1000, tracks ]);
  gbrowseOverviewData["landmarks"].push([ data["started_since_epoch"] * 1000, landmarks ]);

  if (!gbrowseOverviewGraph) {
    gbrowseOverviewGraph = $.plot($("#gbrowseoverviewgraph"), [], {
      series: {
        lines: { show: true },
        points: { show: true }
      },
      xaxis: {
        mode: "time",
        timeformat: "%m/%d/%Y, %H:%M"
      },
      yaxis: {
        min: 0
      }
    });
  }

  gbrowseOverviewGraph.setData([
    { label: "URLs in configuration files", data: gbrowseOverviewData["totalUrls"] },
    { label: "Tested URLs that were broken", data: gbrowseOverviewData["brokenUrls"] },
    { label: "Mismatching images (reference vs. actual)", data: gbrowseOverviewData["imageMismatches"] },
    { label: "URLs that lack a reference image", data: gbrowseOverviewData["missingReferences"] },
    { label: "Tracks", data: gbrowseOverviewData["tracks"] },
    { label: "Example landmarks", data: gbrowseOverviewData["landmarks"] }
  ]);
  gbrowseOverviewGraph.setupGrid();
  gbrowseOverviewGraph.draw();
}

function updateReportDetails(configuration, column, couchData) {
  var data = gbrowseDetailData[configuration] ? gbrowseDetailData[configuration] : {
    totalUrls: [],
    brokenUrls: [],
    imageMismatches: [],
    missingReferences: [],
    tracks: [],
    landmarks: []
  };

  data["totalUrls"].push([ couchData["started_since_epoch"] * 1000, couchData['totalUrls'] ]);
  data["brokenUrls"].push([ couchData["started_since_epoch"] * 1000, couchData['brokenUrls'] ]);
  data["imageMismatches"].push([ couchData["started_since_epoch"] * 1000, couchData['imageMismatches'] ]);
  data["missingReferences"].push([ couchData["started_since_epoch"] * 1000, couchData['missingReferences'] ]);
  data["tracks"].push([ couchData["started_since_epoch"] * 1000, couchData['tracks'] ]);
  data["landmarks"].push([ couchData["started_since_epoch"] * 1000, couchData['landmarks'] ]);
  gbrowseDetailData[configuration] = data;

  var gbrowseDetailGraph = gbrowseDetailGraphs[configuration];
  if (!gbrowseDetailGraph) {
    $("#gbrowsedetailslist").append('<option value="' + configuration + '">' + configuration2Title(configuration) + '</option>');
    $("#gbrowsedetails" + column).append('<h4>' + configuration2Title(configuration) + '</h4><div id="gbrowsedetailgraph_' + configuration + '" class="flotplot"></div>');
    gbrowseDetailGraph = $.plot($("#gbrowsedetailgraph_" + configuration), [], {
      series: {
        lines: { show: true },
        points: { show: true }
      },
      xaxis: {
        mode: "time",
        timeformat: "%m/%d/%Y, %H:%M"
      },
      yaxis: { min: 0 },
      grid: { clickable: true }
    });

    $("#gbrowsedetailgraph_" + configuration).bind("plotclick", function (event, pos, item) {
      if (item) {
          var started_since_epoch = item.datapoint[0];

          window.open(couchBaseUri + "/" + couchBrowsePath + "/" + epoch2URL[configuration + "@" + started_since_epoch]);
      }
    });

    gbrowseDetailGraphs[configuration] = gbrowseDetailGraph;
  }

  epoch2URL[configuration + "@" + (couchData["started_since_epoch"] * 1000)] = couchData["_id"];

  gbrowseDetailGraph.setData([
    { label: "URLs in configuration files", data: data["totalUrls"] },
    { label: "Tested URLs that were broken", data: data["brokenUrls"] },
    { label: "Mismatching images (reference vs. actual)", data: data["imageMismatches"] },
    { label: "URLs that lack a reference image", data: data["missingReferences"] },
    { label: "Tracks", data: data["tracks"] },
    { label: "Example landmarks", data: data["landmarks"] }
  ]);
  gbrowseDetailGraph.setupGrid();
  gbrowseDetailGraph.draw();
}

function createOverviewGraph(data, error) {
  if (error) {
     // TODO
     return;
  }

  var records = data['rows'];
  for (var i = 0; i < records.length; i++) {
    couchRequest(records[i]["id"], getReportOverview);
  }
}

function renderOverview() {
  getReportListing(createOverviewGraph);
}

