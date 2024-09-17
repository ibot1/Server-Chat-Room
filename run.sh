#!/bin/bash

# See README.md for download and installation.
cd target
read -p "Same Process Y/N: " option

if [ "$option" = "Y" ]; then
  echo "Running Same Process scenario!!!"
  sh -c '$java -cp 360T-Coding-Challenge-TK-1-1.0-SNAPSHOT.jar org.example.PlayerApplication isSameProcess=true'
elif [ "$option" = "N" ]; then
  echo "Running different Process scenario!!!"
  sh -c '$java -cp 360T-Coding-Challenge-TK-1-1.0-SNAPSHOT.jar org.example.PlayerApplication isSameProcess=false isInitiator=false initiateePort=8080 playerId=P2 &'
  sh -c '$java -cp 360T-Coding-Challenge-TK-1-1.0-SNAPSHOT.jar org.example.PlayerApplication isSameProcess=false isInitiator=true initiateePort=8080 initiateeHost=localhost playerId=P1'
else
  echo "Made a wrong selection"
fi