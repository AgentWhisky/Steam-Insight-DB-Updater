
# Steam Insight DB Updater

Used to Initialize and keep up-to-date a Steam App Database for use in other Steam API Projects


## Features

- Simple To Understand GUI
- Simplified Connection Process
- Displays Current Status and Update Info
- Run Database Updates every 1-24 hours


## Steam Web API

#### Get Steam App List

```http
  GET https://api.steampowered.com/ISteamApps/GetAppList/v2/
```

#### Get App Info For appid

```http
  GET https://store.steampowered.com/api/appdetails?appids=<appid>
```

| Parameter | Type     | Description                       |
| :-------- | :------- | :-------------------------------- |
| `appid`      | `string` | **Required**. appid of info to fetch |



## Updater In Action

![alt text](readme/non-con.png?raw=true)

![alt text](readme/update-finished.png?raw=true)


