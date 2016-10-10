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
