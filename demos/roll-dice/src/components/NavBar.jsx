import * as React from 'react';
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import IconButton from '@mui/material/IconButton';
import { LanguageSelector } from './LanguageSelector';

export default function NavBar({setGameScreen}) {

  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar position="fixed" sx={{background:'#1e1e1e'}}>
        <Toolbar className='flex items-center justify-between'>
            <IconButton onClick={()=>window.location.reload()}>
              <img src="./RollDiceLogo.svg" alt="" className='h-10 w-10 -mb-2'/>
            </IconButton>
            <LanguageSelector />
        </Toolbar>
      </AppBar>
    </Box>
  );
}
