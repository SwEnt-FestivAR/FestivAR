# FestivAR

[![Contributors](https://img.shields.io/github/contributors/SwEnt-FestivAR/FestivAR)](https://github.com/SwEnt-FestivAR/FestivAR/graphs/contributors)
[![Contributors Activity](https://img.shields.io/github/commit-activity/y/SwEnt-FestivAR/FestivAR)](https://github.com/SwEnt-FestivAR/FestivAR/graphs/contributors)
[![Last Commit](https://img.shields.io/github/last-commit/SwEnt-FestivAR/FestivAR)](https://github.com/SwEnt-FestivAR/FestivAR/commits)
[![Daily Commits](https://img.shields.io/github/commit-activity/d/SwEnt-FestivAR/FestivAR)](https://github.com/SwEnt-FestivAR/FestivAR/graphs/commit-activity)
[![Weekly Commits](https://img.shields.io/github/commit-activity/w/SwEnt-FestivAR/FestivAR)](https://github.com/SwEnt-FestivAR/FestivAR/graphs/commit-activity)
[![Monthly Commits](https://img.shields.io/github/commit-activity/m/SwEnt-FestivAR/FestivAR)](https://github.com/SwEnt-FestivAR/FestivAR/graphs/commit-activity)
[![Yearly Commits](https://img.shields.io/github/commit-activity/y/SwEnt-FestivAR/FestivAR)](https://github.com/SwEnt-FestivAR/FestivAR/graphs/commit-activity)
[![Issues](https://img.shields.io/github/issues/SwEnt-FestivAR/FestivAR)](https://github.com/SwEnt-FestivAR/FestivAR/issues)
[![Open Issues](https://img.shields.io/github/issues/SwEnt-FestivAR/FestivAR/open)](https://github.com/SwEnt-FestivAR/FestivAR/issues)
[![Closed Issues](https://img.shields.io/github/issues-closed/SwEnt-FestivAR/FestivAR)](https://github.com/SwEnt-FestivAR/FestivAR/issues?q=is%3Aissue+is%3Aclosed)
[![Pull Requests](https://img.shields.io/github/issues-pr/SwEnt-FestivAR/FestivAR)](https://github.com/SwEnt-FestivAR/FestivAR/pulls)
[![Closed Pull Requests](https://img.shields.io/github/issues-pr-closed/SwEnt-FestivAR/FestivAR)](https://github.com/SwEnt-FestivAR/FestivAR/pulls?q=is%3Apr+is%3Aclosed)
[![Branches](https://img.shields.io/github/branches/SwEnt-FestivAR/FestivAR)](https://github.com/SwEnt-FestivAR/FestivAR/branches)


## Pitch
Our app proposes itself as a centralized event maker and manager, allowing to prepare the spaces.
The main organizers will have the ability to access a map of the location and place elements on it.
This will allow the association's volunteers to know where every table, marquees and every other structure
should be placed. Moreover, it allows to schedule events, to dispatch roles between all the volunteers
and to assign them tasks. A true event manager. As a secondary feature, some parts can be viewable trough an AR simulation.

---

## Split-app model
We intend to use Firebase cloud service, allowing to handle the management of different accounts containing each user's schedules. The map and information on the event will also be stored with Firebase. This will make each user able to see changes in real time.

---

## Multi-user support
The multi-user support will be supported by Firebase and using google accounts. This will give each user different permissions to modify or just see the event's layout depending on their given role in the association.

---

## Sensor use
The camera will be used for the AR preview and navigation in the event.
The GPS will be used to have better precision in the AR and to access a map of the event.
The gyroscope will be used for the AR.

---

## Offline mode
In offline mode, the app will display cached tasks and schedules for the organizers and let them access the event's map. The AR layout will also be cached.
The app will also be able to load the event from a local file.

---

## Figma link

[Figma](https://www.figma.com/design/hVnXJ5JMVFz0ffuNTlNbJn/FestivAR---Android?node-id=0-1&t=PEh0xqhTGniGAnB8-1)

---

## Code Review
Code reviews should follow these [guidelines](https://github.com/swent-epfl/public/blob/main/project/README.md#reviewing-code).
Don't forget to delete branches after merging in main.

---

## New branches should be named following these guidelines:
- New feature: feature/feature-name (ex: feature/map-screen)
- Bugfix: bugfix/issue-number (ex: bugfix/issue-67)
- Improvements: improvement/short-desc (ex: improvement/refactor)
- Trying things: experiment/desc (ex: experiment/ar-view)
- Tests creation: test/what-is-tested (ex: test/map-gps-loc)

---

## Issues
When creating an issue, make sure to clearly describe the issue and specify the correct labels/types/priority...

---

## Pull requests
Follow the usual commit conventions, as seen in [class](https://drive.google.com/file/d/1JjXkOzGVbd-3MIclaial4zcmi67X0BMc/view).
Add labels as done with issues.

---

<a href="https://github.com/SwEnt-FestivAR/FestivAR/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=SwEnt-FestivAR/FestivAR" alt="Contributors">
</a>
