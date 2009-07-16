#!/usr/bin/env ruby 

require 'rubygems'
require 'trollop'
require 'right_aws'
require 'Ec2' 
include Ec2

aws_access_key_id = get_aws_access_key_id()
aws_secret_access_key = get_aws_secret_access_key()

opts = get_all_options()
Trollop::die :group, " : a group must be specified" if opts[:group] == "unknown"

ec2 = RightAws::Ec2.new(aws_access_key_id,aws_secret_access_key)

instances = ec2.describe_instances()

for instance in instances
  puts instance
  if opts[:group] == instance[:aws_groups][0]
    puts "Terminating #{instance[:aws_instance_id]}"
    ec2.terminate_instances([instance[:aws_instance_id]])
  end
end

