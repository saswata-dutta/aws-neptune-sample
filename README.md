### Build
Creates a fatjar in build/libs.

``gradle jar``

### Deploy
In an aws instance, get the Security Cert.

``wget https://www.amazontrust.com/repository/SFSRootCAG2.pem`` 

### Run

``java -jar neptune-perf-1.0-SNAPSHOT.jar <neptune cluster endpoint> <file with sample keys to lookup> <file to dump times>``

### Sample Data
Try the scripts at [neptune_gremlin_data_gen](https://github.com/saswata-dutta/neptune_gremlin_data_gen).
