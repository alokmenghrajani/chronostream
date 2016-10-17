/**
 * At statup, make an ajax query to /tests/list and build the form.
 */
class Chronostream {
  load() {
    $.ajax({
      url: '/tests/list'
    }).done(data => {
      this.updateForm(data);
    }).fail(err => {
      console.error(err);
      $('#error').text(err);
    });
  }

  /**
   * Once we get the list of algorithms/engines, we can build the form.
   */
  updateForm(data) {
    var form = $("<form/>", {id: "form"});
    var algorithm = $("<select/>", {name: "algorithm"});
    for (var i in data.algorithms) {
      algorithm.append($("<option/>", {value: i}).text(data.algorithms[i]));
    }
    form.append($("<span/>").append(
      $("<label/>").text("algorithm:"),
      algorithm));

    var engine = $("<select/>", {name: "engine"});
    for (var i=0; i<data.engines.length; i++) {
      var e = data.engines[i];
      engine.append($("<option/>", {value: e}).text(e));
    }
    form.append($("<span/>").append(
      $("<label/>").text("engine:"),
      engine));

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
    button.click(e => this.startTest(e));
    form.append($("<span/>").append(button));

    $('#form').replaceWith(form);
  }

  /**
   * Starting a test involves serializing the form and firing an ajax request.
   *
   * The results are gradually streamed from the server.
   */
  startTest(e) {
    e.preventDefault();

    // make an Ajax request to start a test.
    $.ajax({
      url: '/tests/start?' + $('#form').serialize()
    }).done(data => {
      new Result(data.id, data.summary, $('#iterations').val() * $('#threads').val());
    }).fail(err => {
      console.error(err);
      $('#error').text(err);
    });
  }
}

class Result {
  constructor(id, summary, scale) {
    this.id = id;
    this.summary = summary;

    // insert result into page
    this.result = $("<div/>");
    this.result.append($("<p/>").text(this.summary));
    this.result.append($("<div/>", {id: 'graph-'+this.id}));
    this.result.append($("<div/>", {class: "error"}));
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
        .text("foo");

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
    this.fetchResult();
  }

  fetchResult() {
    if (this.done) {
      this.result.find(".status").text("done.");
      return;
    }

    // get more data
    $.ajax({
      url: '/tests/results?id=' + this.id + '&offset=' + this.counter + '&count=50000'
    }).done(data => {
      if (data.exception) {
        console.error(data.exception);
        this.result.find(".error").text(data.exception);
      }
      if (data.total > 0 && this.counter == data.total) {
        this.done = true;
      }

      for (var i = 0; i < data.startEndTimes.length; i++) {
        var v = data.startEndTimes[i].endTime - data.startEndTimes[i].startTime;
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
      this.p.attr("d", this.line)
      this.fetchResult();
    }).fail(err => {
      console.error(err);
      this.result.find(".error").text(err);
    })
  }
}

var chronostream = new Chronostream();
window.addEventListener('load', _ => chronostream.load());
