/**
 * At statup, make an ajax query to /tests/list and build the form.
 */
class Chronostream {
  loadPerf() {
    $.ajax({
      url: '/jobs/list'
    }).done(data => {
      this.updatePerfForm(data);
    }).fail(err => {
      console.error(err);
      $('#error').text(err.responseJSON.message);
    });
  }

  /**
   * Once we get the list of algorithms/engines, we can build the form.
   */
  updatePerfForm(data) {
    var form = $("<form/>", {id: "perf"});
    var primitives = $("<select/>", {name: "primitive"});
    for (var i in data.primitives) {
      primitives.append($("<option/>", {value: i}).text(data.primitives[i]));
    }
    form.append($("<span/>").append(
      $("<label/>").text("primitive:"),
      primitives));

    var providers = $("<select/>", {name: "provider"});
    for (var i=0; i<data.providers.length; i++) {
      var e = data.providers[i];
      providers.append($("<option/>", {value: e}).text(e));
    }
    form.append($("<span/>").append(
      $("<label/>").text("provider:"),
      providers));

    form.append($("<span/>").append(
      $("<label/>").text("bytes:"),
      $("<input/>", {type: "text", name: "bytes", value: 10, size: 5})));

    form.append($("<span/>").append(
      $("<label/>").text("iterations:"),
      $("<input/>", {id: "iterations", type: "text", name: "iterations", value: 1000, size: 7})));

    form.append($("<span/>").append(
      $("<label/>").text("threads:"),
      $("<input/>", {id: "threads", type: "text", name: "threads", value: 100, size: 7})));

    var button = $("<button/>").text("start");
    button.click(e => this.startPerfJob(e));
    form.append($("<span/>").append(button));

    $("#perf").replaceWith(form);

    $("#correctness button").click(e => this.startCorrectnessJob(e));
  }

  startCorrectnessJob(e) {
    e.preventDefault();

    // make an Ajax request to start a test.
    $.post({
      url: '/jobs/startCorrectness',
      data: $('#correctness').serialize()
    }).done(data => {
      console.log(data);
//      new PerfResult(data.id, data.summary, $('#iterations').val() * $('#threads').val());
    }).fail(err => {
      console.error(err);
      $('#error').text(err.responseJSON.message);
    });
  }

  /**
   * Starting a test involves serializing the form and firing an ajax request.
   *
   * The results are gradually streamed from the server.
   */
  startPerfJob(e) {
    e.preventDefault();

    // make an Ajax request to start a test.
    $.post({
      url: '/jobs/startPerf',
      data: $('#perf').serialize()
    }).done(data => {
      new PerfResult(data.id, data.summary, $('#iterations').val() * $('#threads').val());
    }).fail(err => {
      console.error(err);
      $('#error').text(err.responseJSON.message);
    });
  }
}

class PerfResult {
  constructor(id, summary, scale) {
    this.id = id;
    this.summary = summary;

    // insert results into page
    this.result = $("<div/>");
    this.result.append($("<p/>").text(this.summary));
    this.result.append($("<div/>", {id: 'graph-'+this.id}));
    this.result.append($("<pre/>", {class: "error"}));
    this.result.append($("<div/>", {class: "status"}));
    $("#results").append(this.result);

    var margin = {top: 20, right: 20, bottom: 30, left: 50},
        width = 960 - margin.left - margin.right,
        height = 500 - margin.top - margin.bottom;
    this.scaleX = Math.min(scale, 50000);
    this.x = d3.scaleLinear()
        .domain([0,this.scaleX-1])
        .range([0, width]);
    this.y = d3.scaleLinear()
        .domain([0,1])
        .range([height, 0]);
    this.xAxis = d3.axisBottom(this.x);
    this.yAxis = d3.axisLeft(this.y);
    this.line = d3.line()
        .x(d => this.x(d.x))
        .y(d => this.y(d.y));
    this.maxValue = 0;
    this.counter = 0;
    this.data = [];
    this.rawData = [];

    var svg = d3.select("#graph-" + this.id).append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom)
        .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    svg.append("clipPath")
        .attr("id", "innerGraph")
        .append("rect")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom);

    this.axis2 = svg.append("g")
        .attr("class", "y axis")
        .call(this.yAxis)
        .append("text")
          .attr("transform", "rotate(-90)")
          .attr("y", 6)
          .attr("dy", ".71em")
          .style("text-anchor", "end")
          .text("ms");

    this.axis = svg.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(this.xAxis);

    var clip = svg.append("g")
        .attr("clip-path", "url(#innerGraph)")
    this.p = clip.append("path")
          .datum(this.data)
          .attr("class", "line")
          .attr("d", this.line);

    // load data
    this.done = false;
    this.fetch();
  }

  fetch() {
    if (this.done) {
      this.result.find(".status").text("done, computing final results...");
      new FinalPerfResult(this);
      return;
    }

    // get more data
    $.ajax({
      url: '/jobs/perfResult?id=' + this.id + '&offset=' + this.counter + '&count=50000'
    }).done(data => {
      if (data.exception) {
        console.error(data.exception);
        this.result.find(".error").text(data.exception);
        this.done = true;
      }
      if (this.counter == data.total) {
        this.done = true;
      }

      for (var i = 0; i < data.startEndTimes.length; i++) {
        var v = data.startEndTimes[i].endTime - data.startEndTimes[i].startTime;
        this.rawData.push(data.startEndTimes[i]);
        this.data.push({x: this.counter, y: v});
        this.counter++;
        this.maxValue = Math.max(this.maxValue, v);
        if (this.data.length > this.scaleX+1) {
          this.data.shift();
        }
      }

      this.y.domain([0, this.maxValue]);
      d3.selectAll("g.y.axis").call(this.yAxis);

      var l = Math.max(this.counter - this.scaleX, 0);
      this.x.domain([l, l + this.scaleX - 1]);
      d3.selectAll("g.x.axis").call(this.xAxis);
      this.p.attr("d", this.line);
      this.fetch();
    }).fail(err => {
      console.error(err);
      this.result.find(".error").text(err.responseJSON.message);
    })
  }
}

class FinalPerfResult {
  constructor(parent) {
    var latencies = [];
    var start = parent.rawData[0].startTime;
    var end = parent.rawData[0].endTime;
    for (var i=0; i<parent.rawData.length; i++) {
      latencies.push(parent.rawData[i].endTime - parent.rawData[i].startTime)
      start = Math.min(start, parent.rawData[i].startTime);
      end = Math.max(end, parent.rawData[i].endTime)
    }
    latencies = latencies.sort((x, y) => x < y ? -1 : (x == y ? 0 : 1));
    var average_latency = 0;
    for (i=0; i<latencies.length; i++) {
      average_latency += latencies[i];
    }
    average_latency = average_latency / latencies.length;
    var p99_latency = latencies[Math.floor(latencies.length * 0.99)];

    var t = Math.ceil((end - start)/100);
    var throughput = [];
    for (i = start; i<end; i+=t) {
      var n = 0;
      for (var j=0; j<parent.rawData.length; j++) {
        if ((parent.rawData[j].startTime >= i) && (parent.rawData[j].startTime < i + t)) {
          n++;
        }
      }
      throughput.push(n/t*1000);
    }
    throughput = throughput.sort((x, y) => x > y ? -1 : (x == y ? 0 : 1));

    var average_throughput = 0;
    for (i=0; i<throughput.length; i++) {
      average_throughput += throughput[i];
    }
    average_throughput = average_throughput / throughput.length;
    var p99_throughput = throughput[Math.floor(throughput.length * 0.99)];

    var summary = $("<div/>");
    summary.append($("<div/>").text("Latency").append(
      $("<p/>").text("average: " + round(average_latency) + ", min: " +
      latencies[0] + ", p99: " + p99_latency + ", max: " + latencies[latencies.length - 1])));

    summary.append($("<div/>").text("Throughput").append(
      $("<p/>").text("(avg: " + round(average_throughput) + "), min: " +
      round(throughput[0]) + ", p99: " + round(p99_throughput) + ", max: " + round(throughput[throughput.length - 1])
    + ", total: " + round(parent.rawData.length / (end - start) * 1000))));

    parent.result.find(".status").text("");
    parent.result.append(summary);
  }
}

function round(n) {
  return Math.round(n * 1000) / 1000;
}

function debugFake() {
  p = {};
  p.rawData = blah;
  p.result = $('#results');
  return new FinalPerfResult(p);
}
//setTimeout(debugFake, 100);

var chronostream = new Chronostream();
window.addEventListener('load', _ => chronostream.loadPerf());
