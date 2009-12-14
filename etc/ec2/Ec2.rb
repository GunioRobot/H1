module Ec2

  def get_aws_user_id
    aws_user_id = ENV['TALIS_AWS_USER_ID']
    if aws_user_id == nil
      puts "TALIS_AWS_USER_ID not set - exiting"
      exit
    end
    puts "TALIS_AWS_USER_ID found."
    return aws_user_id
  end

  def get_aws_secret_access_key
    aws_secret_access_key = ENV['TALIS_AWS_SECRET_ACCESS_KEY']
    if aws_secret_access_key == nil
      puts "TALIS_AWS_SECRET_ACCESS_KEY not set - exiting"
      exit
    end
    puts "TALIS_AWS_SECRET_ACCESS_KEY found."
    return aws_secret_access_key
  end
  
  def get_aws_access_key_id
    aws_access_key_id = ENV['TALIS_AWS_ACCESS_KEY_ID']
    if aws_access_key_id == nil
      puts "TALIS_AWS_ACCESS_KEY_ID not set - exiting"
      exit
    end    
    puts "TALIS_AWS_ACCESS_KEY_ID found."
    return aws_access_key_id
  end
  
  def get_all_options
    opts = Trollop::options do
      opt :ami, "Amazon Machine Image Name", :default => "unknown"
      opt :num, "Number of Instances", :default => -1
      opt :group, "Security Group Name", :default => "unknown"
      opt :keypair, "Key Pair Name", :default => "eu-kp-1"
      opt :type, "Instance Type", :default => "unknown"
      opt :repo, "Puppet Repository", :default => "unknown"
      opt :dist, "Distribution Directory", :default => "unknown"
      opt :s3, "Deploy dist file to S3 true/false", :default => "unknown"
      opt :puppetmaster, "Puppet Master", :default => "punch.kc.talis.local"
      opt :user, "user to drop as", :default => "puppetdrop"
      opt :password, "Password for ssh session", :default => "unknown"
      opt :startEc2, "Create Ec2 instances, or just describe running ones", :default => "false"
      opt :deploy, "Deploy software by pushing out to the puppet master", :default => "false"
      opt :zone, "Availability zone, e.g. us-east-1b, eu-west-1a, eu-west-1b", :default => nil
      opt :endPoint, "EC2 region (end point for AWS web service), e.g. US or EU", :default => "EU"
    end
    return opts
  end
  
  def validateGroupName(group)
    if group.match(/\./)
      puts "--group parameter must not contain full stops"
      Process.exit
    end    
  end
  
  def get_distribution_name(dist)
    distributionFilename = nil
    Dir.foreach(dist) {
      |name| 
      if name.match('h1-')
        if name.match('.tar.gz')
          distributionFilename = name
        end
      end
    }
    if distributionFilename == nil
      puts "Unable to find h1 distribution - exiting"
      exit
    end
    return distributionFilename
  end
  
  def createGroupIfDoesNotExist(ec2, newGroup, aws_user_id)
    groups = ec2.describe_security_groups()
    exists = false
    for group in groups
      if newGroup == group[:aws_group_name]
        exists = true
      end
    end

    if exists == false
      group = ec2.create_security_group([newGroup],"Development Group")
      ec2.authorize_security_group_IP_ingress(newGroup,22,22)
      ec2.authorize_security_group_IP_ingress(newGroup,9795,9795)
      ec2.authorize_security_group_IP_ingress(newGroup,9696,9696)
      ec2.authorize_security_group_IP_ingress(newGroup,9797,9797)
      ec2.authorize_security_group_IP_ingress(newGroup,8649,8649)
      ec2.authorize_security_group_IP_ingress(newGroup,4444,4444)
      ec2.authorize_security_group_named_ingress(newGroup, aws_user_id, newGroup)
    end    
  end
  
  def createH1PuppetFiles(dns_list, group, repo, distributionFilename, s3)
    libFilename = nil
    libFilename = distributionFilename.split(".")[0] + ".jar"
    filesCreated = Array.new

    puts "Creating puppet files..."
    filename = group+".new"
    myFile = File.new(filename,"w")
    for dns in dns_list
      current_internal_address = dns[1]
      myFile.print current_internal_address
      myFile.puts " "
    end
    myFile.close
    filesCreated << filename

    filename = group+".pp"
    myFile = File.new(filename,"w")

    myFile.puts "node #{group}-basenode {"
    myFile.puts "  $h1Dist = \"#{distributionFilename}\""
    myFile.puts "  $java = \"jre16016\""
    myFile.puts "  $s3 = \"#{s3}\""
    myFile.print "  $h1_internal_ip_list = [ "
    first = true
    for dns in dns_list
      current_internal_address = dns[2]
      if first == false
        myFile.print", "
      end
      first = false
      myFile.print " \""
      myFile.print current_internal_address 
      myFile.print "\""
    end
    myFile.puts " ]"
    myFile.puts "  include env, ganglia"
    myFile.puts "}"
    myFile.puts " "

    zookeeper_server = 1
    for dns in dns_list
      current_internal_name = dns[1]
      myFile.print "node "
      myFile.print "'"
      myFile.print current_internal_name
      myFile.print "' inherits #{group}-basenode { "
      myFile.puts ""
      myFile.puts "  $zookeeper_server = \"#{zookeeper_server}\""
      myFile.puts "  include h1"
      myFile.puts "} "
      zookeeper_server = zookeeper_server + 1
    end
    myFile.close 
    filesCreated << filename
    
    return filesCreated
  end
  
  def deploy(puppetFiles, majatDist, puppetmaster, user, password, s3, aws_access_key_id, aws_secret_access_key)
    if s3 == "false"
      begin
        Net::SCP.start(puppetmaster, user, :password => password) do |scp|
          scp.upload! majatDist, "/puppetdrop/files"
          for file in puppetFiles
            puts "uploading #{file} to puppet..."
            scp.upload! file, "/var/www/html/chroot/puppetdrop/drop"
          end
        end
      rescue
        puts "FAILED TO SCP PUPPET FILES: " + $!
        Process.exit
      end
    end

    if s3 == "true"
      begin
        Net::SCP.start(puppetmaster, user, :password => password) do |scp|
          for file in puppetFiles
            puts "uploading #{file} to puppet..."
            scp.upload! file, "/var/www/html/chroot/puppetdrop/drop"
          end
        end
      rescue
        puts "FAILED TO SCP PUPPET FILES: " + $!
        Process.exit
      end
      
      bucket = "majat"
      s3 = RightAws::S3.new(aws_access_key_id, aws_secret_access_key)
      begin 
        bucket1 = s3.bucket(bucket, false)
        bucket1.keys
      rescue Exception => e
        puts "Bucket does not exist" + "\n" + e
        Process.exit
      end
      base_name = File.basename(majatDist)
      puts "Uploading #{majatDist} as '#{base_name}' to '#{bucket}'"
      begin
        key = RightAws::S3::Key.create(bucket1, base_name)
        key.put(open(majatDist))
      rescue Exception => e
        puts "Failed uploading file" + "\n" + e
        Process.exit
      end
    end    
  end
  
  def internalNameToIpAddress(internal_name)
    internal_ip = String.new(str=internal_name)
    internal_ip["ip-"] = ""
    internal_ip[".eu-west-1.compute.internal"] = ""
    internal_ip["-"] = "."
    internal_ip["-"] = "."
    internal_ip["-"] = "."
    return internal_ip
  end
  
end
