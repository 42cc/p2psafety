describe('hello test', function() {

    var scope = null;

    beforeEach( module('mapApp') );
    beforeEach( inject(function($controller, $rootScope, $httpBackend, $interval) {
            scope = $rootScope.$new();
            window.google = {
                maps: jasmine.createSpyObj('googleMaps', ['LatLngBounds','LatLng','Map','InfoWindow'])
            };
            window.google.maps.ControlPosition = {
                RIGHT_CENTER: ''
            };
            $httpBackend.when('GET', '/mock/').respond({objects:{}});
            $controller('EventListCtrl', {
                $scope: scope,
                urls: {
                    events: '/mock/',
                    roles: '/mock/',
                    movement_types: '/mock/'
                },
                mapSettings: {},
                ensurePathi: {}
            });
    }) );


    it('should pass', function(){
        expect(true).toBe(true)
    })
})
