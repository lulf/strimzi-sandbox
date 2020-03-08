import React, { Component } from 'react';
import Keycloak from 'keycloak-js';
import './App.css';
import { BrowserRouter, Route, NavLink, Switch } from 'react-router-dom';

class Dashboard extends Component {
    constructor(props) {
        super(props);
        this.state = { keycloak: null, authenticated: false };
    }

    componentDidMount() {
        const keycloak = Keycloak('/keycloak.json');
        keycloak.init({onLoad: 'check-sso', promiseType: 'native'}).then(authenticated => {
            var self = this;
            var state = { keycloak: keycloak, authenticated: authenticated };
            if (authenticated) {
                var token = keycloak.token;
                keycloak.loadUserProfile().then(function (profile) {
                    fetch('http://localhost:8080/api/tenants/' + profile.username, {
                        crossDomain: true,
                        method: 'GET',
                        headers: {
                            'Accept': 'application/json',
                            'Content-Type': 'application/json',
                            'Authorization': "Bearer " + token,
                        },
                    }).then((response) => {
                        console.log("Response: " + response.status);
                        return response.json();
                    }).then((data) => {
                        state.tenant = data;
                        self.setState(state);
                    }).catch(function () {
                        self.setState(state);
                    });
                }).catch(function() {
                    this.setState(state);
                });
            }
        });
    }

    render() {
        if (this.state.keycloak) {
            if (this.state.authenticated) {
                if (this.state.tenant !== undefined) {
                    var creationTimestamp = Date.parse(this.state.tenant.creationTimestamp);
                    var creationDate = new Date(creationTimestamp);
                    var creationDateStr = creationDate.toLocaleString();
                    if (this.state.tenant.provisionTimestamp !== undefined) {
                        var provisionTimestamp = Date.parse(this.state.tenant.provisionTimestamp);
                        var provisionDate = new Date(provisionTimestamp);
                        var provisionDateStr = provisionDate.toLocaleString();
                        var expirationTimestamp = provisionTimestamp + (7 * 24 * 60 * 60 * 1000);
                        var now = Date.now();
                        var timeUntilDeletion = (expirationTimestamp - now) / 1000;
                        var expireDays = Math.floor(timeUntilDeletion / (3600 * 24));
                        var expireHours = Math.floor(timeUntilDeletion % (3600 * 24) / 3600);
                        return (
                                <div className="App">
                                <h3>Status</h3>
                                <p>Logged in as {this.state.tenant.name}</p>
                                <p>Registered at {creationDateStr}</p>
                                <p>Provisioned at {provisionDateStr} (Expires in {expireDays} days and {expireHours} hours)</p>
                                <p>Console: <a href="https://console.sandbox.enmasse.io">https://console.sandbox.enmasse.io</a></p>
                            </div>
                        );
                        
                    } else {
                        return (
                                <div className="App">
                                <h3>Status</h3>
                                <p>Logged in as {this.state.tenant.name}</p>
                                <p>Registered at {creationDateStr}</p>
                                <p>Environment not yet provisioned</p>
                                </div>
                        );
                    }
                } else {
                    return (
                            <div className="App">
                            <h3>Not registered</h3>
                            <NavLink to="/">Back</NavLink>
                            </div>
                    );
                }
            } else {
                return (
                        <div className="App">
                        <h3>Account not registered</h3>
                        <NavLink to="/">Back</NavLink>
                        </div>
                );
            }
        }
        return (
                <div className="App">
                <h3>Account not registered</h3>
                <NavLink to="/">Back</NavLink>
                </div>
        );
    }
};

export default Dashboard;
