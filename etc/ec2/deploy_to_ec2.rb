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

userdata = "PUPPETCONFIG: \"[main]\\nconfdir=/etc/puppet\\nvardir=/var/lib/puppet\\nlibdir=/etc/puppet/lib\\nssldir=$vardir/ssl\\nmanifest=/etc/puppet/manifests/site.pp\\nmodulepath=/etc/puppet/modules\\ntemplatedir=$confdir/templates\\nfiletimeout=0\\n\\n[puppetd]\\nserver=punch.kc.talis.com\\npuppetport=8139\\nruninterval=120\\nssldir=/etc/puppet/ssl\"\n"
userdata = userdata + "\n" + "PUPPETURL: \"http://talis-distros.s3.amazonaws.com/puppet/puppet-0.25.1.tar\""

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
  instances = ec2.run_instances(opts[:ami],opts[:num],opts[:num],opts[:group],opts[:keypair], userdata, 'public', opts[:type], nil, nil, opts[:zone], nil)
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
    puts "Started #{started_instances.length} instances."
    started = true
  else
    puts "Started #{started_instances.length} instances, #{pending_instances.length} still pending. Waiting... "
    checks_before_offer_optout = 1
    started_instances.clear
    pending_instances.clear
    other_instances.clear
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
    instance_list << internalNameToIpAddress(instance_description[:private_dns_name])
    dns_list << instance_list
  end
end

puppetFiles = createH1PuppetFiles(dns_list, opts[:group], opts[:repo], distributionFilename, opts[:s3])
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