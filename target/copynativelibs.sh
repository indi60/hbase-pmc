if [ `ls /media/data/research/cpn/hbase-static-analysis/hb-3483/hbase-0.94.1/target/nativelib | wc -l` -ne 0 ]; then
                      cp -PR /media/data/research/cpn/hbase-static-analysis/hb-3483/hbase-0.94.1/target/nativelib/lib* /media/data/research/cpn/hbase-static-analysis/hb-3483/hbase-0.94.1/target/hbase-0.94.1/hbase-0.94.1/lib/native/Linux-amd64-64
                    fi