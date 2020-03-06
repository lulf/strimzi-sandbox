import React, { Component } from 'react';
import Keycloak from 'keycloak-js';
import './App.css';

class Dashboard extends Component {
  constructor(props) {
    super(props);
    this.state = { keycloak: null, authenticated: false };
  }

  componentDidMount() {
    const keycloak = Keycloak('/keycloak.json');
    keycloak.init({onLoad: 'login-required', promiseType: 'native'}).then(authenticated => {
      this.setState({ keycloak: keycloak, authenticated: authenticated })
    })
  }

  render() {
    if (this.state.keycloak) {
      if (this.state.authenticated) return (
        <div className="App">
          <h3>Logged in</h3>
        </div>
      ); else return (
        <div className="App">
          <h3>Not logged in</h3>
        </div>
      )
    }
    return (
      <div>Initializing Keycloak...</div>
    );
  }
};

export default Dashboard;
