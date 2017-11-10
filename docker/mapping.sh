#!/bin/bash

curl -XPUT "localhost:9200/comms" -H 'Content-Type: application/json' -d '
{
    "mappings": {
        "message": {
            "properties": {
                "title":    { "type": "text"  }, 
                "type":     { "type": "keyword" },
                "body":     { "type": "text"  }, 
                "senders":  { "type":   "text"  },
                "receivers": {  "type":   "text"  },
                "timestamp":  {
                    "type":   "date", 
                    "format": "strict_date_optional_time||epoch_millis"
                }
            }
        }
    }
}'