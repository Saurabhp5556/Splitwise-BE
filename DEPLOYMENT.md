# Deployment Setup

## One-Time Server Setup

Run these commands on your VPS to set up the systemd service:

```bash
# 1. Create app directory
mkdir -p /app

# 2. Install required tools
sudo apt-get update && sudo apt-get install -y psmisc

# 3. Copy the systemd service file to the server
# (Upload splitwise.service from this repository)
sudo cp splitwise.service /etc/systemd/system/

# 4. Reload systemd and enable the service
sudo systemctl daemon-reload
sudo systemctl enable splitwise

# 5. Clean up any existing processes and start
sudo pkill -9 -f "Splitwise-BE.jar" || true
sudo systemctl start splitwise

# 6. Verify it's running
sudo systemctl status splitwise
```

## Managing the Service

```bash
# Check status
sudo systemctl status splitwise

# View logs
sudo journalctl -u splitwise -f

# Restart service
sudo systemctl restart splitwise

# Stop service
sudo systemctl stop splitwise
```

## Automated Deployment

Once the service is set up, deployments are automatic:

1. Push code to GitHub
2. Run the "Deploy Backend" workflow from GitHub Actions
3. The workflow will:
   - Build the JAR
   - Copy it to `/app/Splitwise-BE.jar`
   - Restart the systemd service
   - Verify it started successfully

## Troubleshooting

### Port Already in Use

If you see "Port 8081 was already in use":

```bash
# Stop the service
sudo systemctl stop splitwise

# Kill all Java processes
sudo pkill -9 -f "Splitwise-BE.jar"

# Kill anything on port 8081
sudo fuser -k 8081/tcp

# Restart the service
sudo systemctl start splitwise
```

### General Issues

```bash
# Check service status
sudo systemctl status splitwise

# View recent logs (systemd)
sudo journalctl -u splitwise -n 50

# View application logs
tail -f /app/app.log

# Check if port is in use
sudo netstat -tulpn | grep :8081

# Manually restart
sudo systemctl restart splitwise
```
