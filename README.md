# charge2GO

<p align="center">
  <img src="https://github.com/hashbangers/charge2GO/blob/master/screenshots/charge2GO.png" width="230">
</p>

**charge2GO** is a smart barter system for the exchange of charge between electric cars to reduce downtime.

# Current Problem

All electric vehicle owners suffer from *Range Anxiety*. Range Anxiety can be described as follows: you never know when you might run out of charge; and when you do, the nearest unoccupied charging station might be be unreachable/unavailable.

<p align="center">
  <img src="https://github.com/hashbangers/charge2GO/blob/master/screenshots/problem_map.png" width="250">
</p>

---

# Proposed Solution

<p align="center">
  <img src="https://github.com/hashbangers/charge2GO/blob/master/screenshots/merc_image.png" width="700">
</p>

What if every electric car on the road was a potential charging station? This way vehicle owners can obtain charge not only from other charging stations but also from stationary cars on the road. The donor (the owner of the vehicle willing to donate charge) gets an incentive based on the number of units of charge he/she donates.

<p align="center">
  <img src="https://github.com/hashbangers/charge2GO/blob/master/screenshots/solution_map.png" width="250">
</p>

---

# How to use the application

## Travel to destination

Right after the app launches, the user enters his name and the units of charge he has at the start of the journey. He can also select the type/model of car he is travelling in. Next, on the map, he chooses the destination location and starts his travel.


## Request for charge

Once his charge drops below a certain threshold value, a pop-up appears saying that he can request for charge. In the pop-up, he enters the units of charge he requires. This request message is then sent to all the neighbouring cars in a 4-mile radius.
  
<p align="center">
  <img src="https://github.com/hashbangers/charge2GO/blob/master/screenshots/request_Interface.jpeg" width="250">
 &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
  <img src="https://github.com/hashbangers/charge2GO/blob/master/screenshots/requesting_state.jpeg" width="250">
</p>

## Respond to request for charge

If a car is within the 4-mile radius of the requesting car and has enough charge, a pop-up will appear on the donor's side showing him the location of the requesting car and the units of charge requested. Along with this, the max units of charge that he can donate is displayed in the pop-up. In this pop-up, the donor can enter the units of charge he is willing to donate - based on this, the credit points that he will receive on completing the *charge transaction* is shown to him.
  
<p align="center">
  <img src="https://github.com/hashbangers/charge2GO/blob/master/screenshots/response_Interface.jpeg" width="250">
</p>

## Travel to donor

Once the donor accepts the request, an OTP is sent to the requesting car. The requesting car travels to the location of the donor car, keys in the OTP and thus unlocks the hatch to gain access to the charging mechanism.

<p align="center">
  <img src="https://github.com/hashbangers/charge2GO/blob/master/screenshots/travelling_state.jpeg" width="250">
</p>

---

# Built With

* [Java](https://java.com/en/) 
* [Android Studio](https://developer.android.com/index.html) Application development
* [Photoshop](www.adobe.com/Photoshop) creation of icons and images
* [Firebase](https://firebase.google.com) Database 
* [Google Maps API](https://developers.google.com/maps) 

---

# Team Members

* [Abhijeeth Padarthi](https://github.com/rkinabhi)
* [Arkoprabho Bhattacharjee](https://github.com/)
* [Aravind Subramaniam](https://github.com/aravind098)
* [Ayush Agrawal](https://github.com/ayush2098)

---

# License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details




