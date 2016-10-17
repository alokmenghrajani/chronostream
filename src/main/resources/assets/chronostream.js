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
      $("<input/>", {type: "text", name: "iterations", value: 1000, size: 7})));

    form.append($("<span/>").append(
      $("<label/>").text("threads:"),
      $("<input/>", {type: "text", name: "threads", value: 100, size: 7})));

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
      console.log("DONE!");
      window.Alok = data;
//      var r = this.state.results.slice()
//      r.push(<CryptoResult key={data.id} id={data.id}/>);
//      this.setState({results: r})
    }).fail(err => {
      console.error(err);
      $('#error').text(err);
    });
  }
}

var chronostream = new Chronostream();
window.addEventListener('load', _ => chronostream.load());
