#!/usr/bin/env ruby 

require 'rubygems'
require 'trollop'
require 'right_aws'
require 'readline'
require 'net/scp'
require 'Ec2' 
include Ec2

aws_access_key_id = get_aws_access_key_id()
aws_secret_access_key = get_aws_secret_access_key()
aws_user_id = get_aws_user_id()

opts = get_all_options()

# make sure we use the ec2 EU region
params = { :server => "eu-west-1.ec2.amazonaws.com" }
ec2 = RightAws::Ec2.new(aws_access_key_id, aws_secret_access_key, params)
zones = ec2.describe_availability_zones

puts params[:server]

p zones
p params


s3 = RightAws::S3.new(aws_access_key_id, aws_secret_access_key)
buckets = s3.buckets
p buckets

puts ""

for bucket in buckets
  puts "Bucket: #{bucket.name} Location: #{bucket.location}"
end
