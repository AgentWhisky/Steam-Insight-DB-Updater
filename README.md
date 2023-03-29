# SteamDB-Updater


## Purpose

- Part 1 of 2 - Steam WebApp Project
- This Updater Is Built To Initialize and Keep Up-To-Date, A Table in a Database

## Functions

- Create Table 'AppInfo' In Connected Database if necessary
- Add an Up-To-Date List of all Steam Apps from Its API to the Database
- Update all Out-Of-Date apps in the Database to ensure valid data
  - WARNING: Due To How The Steam Web API is set up, in order to know any information other than an app's appid and name, a separate call for EACH appid mus be made. This is not only incredibly inefficient but also time-consuming as only 200 Calls per 5 minutes can be made. No better method has been found.
  - Once the Database is Up-To-Date the first time, subsequent updates should be simple and quick

- Later Versions of this project may include a basic .csv file for far faster database setup

-------------------------------------------
## Config
- A config.properties file must be added to src/main/java/resources
- It needs the following information:

  - db.address=
  - db.port=
  - db.name=
  - db.user=
  - db.password=
  - steamAPI.key= Steam_api_key

  

-------------------------------------------
## Steam Web API Reference

#### Get Steam App List (v2)

```http
  GET https://api.steampowered.com/ISteamApps/GetAppList/v2/?key=<STEAM_API_KEY>
```

| Parameter | Type     | Description                              |
| :-------- | :------- |:-----------------------------------------|
| `STEAM_API_KEY` | `string` | **Required** -  An API Supplied by Steam |

#### Get App Details

```http
  GET https://store.steampowered.com/api/appdetails?appids=<appid>
```

| Parameter | Type     | Description                              |
| :-------- | :------- |:-----------------------------------------|
| `appid`      | `string` | **Required** -  appid to get details for |


