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

Trollop::die :ami, " : an amazon machine image must be specified" if opts[:ami] == "unknown"
Trollop::die :num, " : a number of instances must be specified" if opts[:num] == -1
Trollop::die :group, " : a group must be specified" if opts[:group] == "unknown"
Trollop::die :type, " : a type must be specified e.g. m1.small" if opts[:type] == "unknown"
Trollop::die :repo, " : the puppet repostitory must be specified e.g. majat.s3.amazonaws.com" if opts[:repo] == "unknown"
Trollop::die :dist, " : a distribution directory must be specified e.g. ../../build/dist" if opts[:dist] == "unknown"
Trollop::die :s3, " : deploy dist file to S3? true or false" if opts[:s3] == "unknown"

validateGroupName(opts[:group])

params = { :server => "eu-west-1.ec2.amazonaws.com" }
if opts[:endPoint] == "US"
  # aws uses US endpoint by default if no params supplied
  params = {}
end
ec2 = RightAws::Ec2.new(aws_access_key_id, aws_secret_access_key, params)

majatDist = nil
distributionFilename = get_distribution_name(opts[:dist]) 
majatDist = opts[:dist] + "/" + distributionFilename

createGroupIfDoesNotExist(ec2, opts[:group], aws_user_id)

if opts[:startEc2] == "true"
  puts "Starting Ec2 instances..."
  instances = ec2.run_instances(opts[:ami],opts[:num],opts[:num],opts[:group],opts[:keypair], '', 'public', opts[:type], nil, nil, opts[:zone], nil)
else
  puts "Describing running Ec2 instances..."
  instances = ec2.describe_instances()
end

started_instances = Array.new
pending_instances = Array.new
other_instances = Array.new
started = false
checks_before_offer_optout = 1
while started == false
  sleep 30
  for instance in instances
    if instance[:aws_groups][0].eql? opts[:group]
      instance_description = ec2.describe_instances([instance[:aws_instance_id]])[0]
      if instance_description[:aws_state] == "running"
        started_instances.push(instance)
      elsif instance_description[:aws_state] == "pending"
        pending_instances.push(instance)
      else
        other_instances.push(instance)
      end
    end
  end
  if started_instances.length >= opts[:num]
    started = true
  else
    checks_before_offer_optout = checks_before_offer_optout -1
    if checks_before_offer_optout == 0
      puts "Started #{started_instances.length} instances, #{pending_instances.length} still pending."
      puts "Would you like to start with just the #{started_instances.length} instances?"
      optout = readline() 
      if optout == "y\n" or optout == "Y\n"
        started = true
      else
        puts "Started #{started_instances.length} instances, #{pending_instances.length} still pending. Waiting... "
        checks_before_offer_optout = 1
        started_instances.clear
        pending_instances.clear
        other_instances.clear
      end
    end
  end
end
puts "Started #{started_instances.length} instances."

dns_list = Array.new
for instance in started_instances
  instance_description = ec2.describe_instances([instance[:aws_instance_id]])[0]
  if instance_description[:aws_state] == "running"
    instance_description = ec2.describe_instances([instance[:aws_instance_id]])[0]
    instance_list = Array.new
    instance_list << instance_description[:dns_name]
    instance_list << instance_description[:private_dns_name]
    dns_list << instance_list
  end
end

puppetFiles = createPuppetFiles(dns_list, opts[:group], opts[:repo], distributionFilename, opts[:s3])
puts "Created puppet files:"
p puppetFiles

if opts[:deploy] == "true"
  deploy(
    puppetFiles, 
    majatDist, 
    opts[:puppetmaster], 
    opts[:user], 
    opts[:password], 
    opts[:s3], 
    aws_access_key_id, 
    aws_secret_access_key)
end

puts "Creating seed file #{opts[:seedfile]} ..."
createSeedFile(dns_list, opts[:seedfile], 0, dns_list.size-1)

puts "Creating proxy file #{opts[:proxyfile]} ..."
createProxyFile(dns_list, opts[:proxyfile])

puts "Creating address file #{opts[:addressfile]} ..."
createAddressFile(dns_list, opts[:addressfile])

puts "Creating demon nodes file #{opts[:demonodes]} ..."
createdemoNodesFile(dns_list, opts[:demonodes])

puts "Creating monitoring file #{opts[:monitoringfile]}..."
createMonitoringFile(dns_list, opts[:monitoringfile])

breakpoint = dns_list.size / 2

puts "Creating SeedFirstHalf file ..."
createSeedFile(dns_list, "SeedFirstHalf", 0, breakpoint-1)

puts "Creating SeedSecondHalf file ..."
createSeedFile(dns_list, "SeedSecondHalf", breakpoint, dns_list.size-1)

puts "Creating SeedJoin file ..."
createSeedFile(dns_list, "SeedJoin", breakpoint-1, breakpoint)

puts "Creating cssh file #{opts[:csshfile]}..."
createCssFile(dns_list, opts[:csshfile], 0, (dns_list.size-1))

puts "Creating cssh_first_half.sh file ..."
createCssFile(dns_list, "cssh_first_half.sh", 0, (breakpoint-1))

puts "Creating cssh_second_half.sh file ..."
createCssFile(dns_list, "cssh_second_half.sh", breakpoint, (dns_list.size-1))



