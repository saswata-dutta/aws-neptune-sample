#!/usr/bin/env bash

readonly role=$(curl -s "http://169.254.169.254/latest/meta-data/iam/security-credentials/")
readonly response=$(curl -s "http://169.254.169.254/latest/meta-data/iam/security-credentials/$role")

readonly code=$(echo "$response" | jq '.Code' -r)

[[ "Success" == "$code" ]] || {
  echo "Error !!! Code is $code"
  exit 1
}

AWS_ACCESS_KEY_ID=$(echo "$response" | jq '.AccessKeyId' -r)
AWS_SECRET_ACCESS_KEY=$(echo "$response" | jq '.SecretAccessKey' -r)
AWS_SESSION_TOKEN=$(echo "$response" | jq '.Token' -r)

export AWS_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY
export AWS_SESSION_TOKEN
export SERVICE_REGION='us-west-2'

echo "$AWS_ACCESS_KEY_ID"
echo "$AWS_SECRET_ACCESS_KEY"
echo "$AWS_SESSION_TOKEN"
echo "$SERVICE_REGION"

gradle jar

readonly neptune="$1"
readonly input="$2"
java -jar build/libs/neptune-perf-1.0-SNAPSHOT.jar \
  "$neptune" 'SFSRootCAG2.ks' '123456' "$input" 'out.txt'
