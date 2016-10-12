Must do:
- some endpoint to download data in R format or simply write to disk after N minutes?
  - design an experiment and gather data.
    - AES, 128bit, x bytes, encryption, y threads
      - average latency, throughput, etc.
    - AES, 256bit, x bytes, encryption, y threads

    - etc.
- correctness checks

Nice to have:
- make things easier to understand (better names) + some kind of code re-use.
  (e.g. a job runs Test multiple times and collects stats).
- save test results to H2.
- load previous results from H2.
- add some correctness tests
- figure out better graphing story
- plot network graph?
- plot GC pauses
- handle bytes parameter
- store test parameters / display them better
- copy remote assets to assets/
- better dev/prod story for assets
- document the stack/how this works:

  Java service
    |
     -- schedule multiple threads to run a test
    |
     -- h2 to store results
    |
     -- websocket to stream results (TODO)
    |
     -- javascript to render results in real-time (React + d3)
    |
     -- ability to download data as R file (TODO)
