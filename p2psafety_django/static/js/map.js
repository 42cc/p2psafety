var mapApp = angular.module('mapApp', ["angular-lodash","ngAnimate"]);

mapApp.constant('ICONS', {
  RED: 'http://maps.google.com/mapfiles/ms/micons/red-dot.png',
  GREEN: 'http://maps.google.com/mapfiles/ms/micons/green-dot.png',
  BLUE: 'http://maps.google.com/mapfiles/ms/micons/blue-dot.png',
});

mapApp.controller('EventListCtrl', function($scope, $http, $interval, urls, mapSettings) {
  $scope.initGoogleMap = function(rootElement) {
    var mapOptions = {
      zoom: 10,
      center: new google.maps.LatLng(50.444, 390.56),
      zoomControl: true,
      zoomControlOptions: {position: google.maps.ControlPosition.RIGHT_CENTER},
      panControl: true,
      panControlOptions: {position: google.maps.ControlPosition.RIGHT_TOP},
    };
    $scope.gmap = new google.maps.Map(rootElement, mapOptions);
    $scope.gwindow = new google.maps.InfoWindow({content: "Sup"});
    $scope.gwindow_opened = false;
  };
  $scope.zoomIn = function() {
    if ($scope.zoomedIn == false) {
      $scope.gmap.setZoom($scope.gmap.getZoom() + $scope.zoomScale);
      $scope.zoomedIn = true;
    }
  };
  $scope.zoomOut = function() {
    if ($scope.zoomedIn == true) {
      $scope.gmap.setZoom($scope.gmap.getZoom() - $scope.zoomScale);
      $scope.zoomedIn = false;
    }
  };
  $scope.select = function(event) {
    if (event == null) {
      $scope.zoomOut();
      $scope.selectedEvent = null;
    } else {
      var params = {event__id: event.id};
      $http.get(urls.eventupdates, {params: params}).success(function(data) {
        event.updates = data.objects;
        $scope.focus(event.latest_location);
        $scope.zoomIn();
        $scope.selectedEvent = event;
        $scope.selectedEvent.isNew = false;
      });
    };
  };
  $scope.update = function(options, callback) {
    var params = {status: 'A'};
    $http.get(urls.events, {params: params}).success(function(data) {
      var eventsAppeared = false, newEvents = {};
      for (i in data.objects) {
        var event = data.objects[i];


        newEvents[event.id] = event;
      }
      // Deleting old events
      for (oldEventId in $scope.events) {
        if (newEvents[oldEventId] == null) {
          delete $scope.events[oldEventId];
        }
      }
      // Adding new events
      for (newEventId in newEvents) {
        if ($scope.events[newEventId] == null) {
            var new_event =  newEvents[newEventId]
            if (options.highightNew){
                new_event.isNew = true
            } else {
                new_event.isNew = false
            }
          new_event.isVisible = true;
          $scope.events[newEventId] = new_event;
          eventsAppeared = true;
        }
      }
      if (eventsAppeared && options.playSoundForNew)
        document.getElementById('audiotag').play();

      // Making map show all events
      if (options.centerMap) {
        var eventsBounds = new google.maps.LatLngBounds();
        for (eventId in $scope.events) {
          var event = $scope.events[eventId];
          eventsBounds.extend(new google.maps.LatLng(
            event.latest_location.latitude,
            event.latest_location.longitude
          ));
        }
        $scope.gmap.fitBounds(eventsBounds);


      }
      if ('function' === typeof callback) callback();
    });
  };

  $scope.updateUserAttrs = function(){
        //clojure for ajax callback
      _.forEach($scope.events, function(event){
        var clojr = function(pevent,field){
            return function(data){
                pevent.user[field] = data;
            }
        }

        $http.get(urls.user_roles, {params:{id:event.user.id}}).success(clojr(event,'roles'))
      });
    }

  $scope.focus = function(location) {
    $scope.gmap.panTo(new google.maps.LatLng(location.latitude,
                                             location.longitude));
  };

  $scope.getRoles = function() {
      //populate list of avail roles for matching
      $scope.roles={};
      $http.get(urls.roles).success(function(data) {
          _.forEach(data.objects, function(role){
              $scope.roles[role.id] = role;
              $scope.visibleRoles.role.push(role.id)
          });
      });
  }

  $scope.getMovementTypes = function() {
      //populate list of avail movement_types for matching
      $http.get(urls.movement_types).success(function(data) {
          $scope.movement_types = data.objects;
      });
  }


  $scope.toggleFilter = function(filter) {
      if (_.contains($scope.visibleRoles[filter.type],filter.id)){
          _.pull($scope.visibleRoles[filter.type],filter.id)
      }else{
          $scope.visibleRoles[filter.type].push(filter.id)
      }
      _.forEach($scope.events, function(event){
          if(!_.isEmpty(event.user.roles) && _.isEmpty(_.intersection(
                      event.user.roles, $scope.visibleRoles['role']))){
              event.isVisible = false;
          }else{
              event.isVisible = true
          }
      });
  }

  $scope.selectedEvent = null;
  $scope.zoomedIn = false;
  $scope.zoomScale = 1;
  $scope.initGoogleMap(document.getElementById("map-canvas"));
  $scope.events = {};
  $scope.getRoles();
  $scope.filterPanel = false;
  //$scope.getMovementTypes();
  $scope.updatePerSeconds = 5;

  $scope.visibleRoles = {'role':[],'movement_type':[]};
  
  $scope.update({playSoundForNew:false, highightNew:false, centerMap:true},
          $scope.updateUserAttrs
  );

  $interval(function() {
    $scope.update({playSoundForNew:mapSettings.sound,
        highightNew:mapSettings.highlight,
        centerMap:false});
  }, $scope.updatePerSeconds * 1000);

  $interval(function() {
    $scope.updateUserAttrs();
  },15*1000);
})
.factory('markerFactory', function() {
  return function(scope, element, content, icon, location, map, onclick) {    
    var markerArgs = {
      icon: icon,
      position: new google.maps.LatLng(location.latitude, location.longitude),
    };

    var marker = new google.maps.Marker(markerArgs);
    marker.setMap(map);
    if (onclick)
      google.maps.event.addListener(marker, 'click', function() {
        scope.$eval(onclick);
      });

    if (content.length) {
      content = content.detach()[0];
    }
    var markersWindow = new google.maps.InfoWindow();
      
    google.maps.event.addListener(marker, 'mouseover', function() {
      markersWindow.setContent(content);
      markersWindow.open(map, marker);
    });
    google.maps.event.addListener(marker, 'mouseout', function() {
      if (markersWindow != null) markersWindow.close();
    });

    element.on('$destroy', function() {
      marker.setMap(null);
      marker = null;
      if (markersWindow != null) {
        markersWindow.close();
        markersWindow = null;
      }
    });

    return marker;
  };
})
.directive('eventMarker', function(markerFactory, ICONS) {
  var linker = function(scope, element, attrs) {
    var location = scope.event.latest_location;

    if (location) {
      var map = scope.$parent.gmap;
      var icon = (scope.event.type == 'victim') ? ICONS.RED : ICONS.GREEN;
      var content = element.children().detach()[0];
      var marker = markerFactory(scope, element, content, icon, location,
                                 map, attrs.click);

      scope.$watch('event.isNew', function(isNew) {
        var animation = (isNew) ? google.maps.Animation.BOUNCE : null;
        marker.setAnimation(animation);
      });
      scope.$watch('event.isVisible', function (isVisible) {
          if (isVisible) marker.setVisible(true)
          else marker.setVisible(false);
      });
    }
  };
  return {
    replace: true,
    template: '',
    restrict: 'E',
    link: linker,
  };
})
.directive('eventupdateMarker', function(markerFactory, ICONS) {
  var linker = function(scope, element, attrs) {
    if (scope.update.location != null) {
      var map = scope.$parent.gmap;
      var content = element.children().detach()[0];
      var marker = markerFactory(scope, element, content, ICONS.BLUE,
                                 scope.update.location, map);
    }
  };
  return {
    replace: true,
    template: '',
    restrict: 'E',
    link: linker,
  };
});
