# Deployment Setup

## One-Time Server Setup

Run these commands on your VPS to set up the systemd service:

```bash
# 1. Create app directory
mkdir -p /app

# 2. Copy the systemd service file to the server
# (Upload splitwise.service from this repository)
sudo cp splitwise.service /etc/systemd/system/

# 3. Reload systemd and enable the service
sudo systemctl daemon-reload
sudo systemctl enable splitwise
sudo systemctl start splitwise

# 4. Verify it's running
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

If deployment fails:

```bash
# Check service status
sudo systemctl status splitwise

# View recent logs
sudo journalctl -u splitwise -n 50

# Check if port is in use
sudo netstat -tulpn | grep :8080

# Manually restart
sudo systemctl restart splitwise
```
