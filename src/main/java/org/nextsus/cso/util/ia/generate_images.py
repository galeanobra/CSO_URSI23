import numpy as np
import matplotlib.pyplot as plt
import matplotlib.colors as colors
import statistics as st
import os

os.chdir('../../../../../../../../')

max = 9999

default_raw = 'data/ia/raw/'
default_img = 'data/ia/img/'
default_img_inter = 'data/ia/img_interpolation/'

# Leer data/ia/raw/users
# Leer data/ia/raw/pathloss
# Recorrer data/ia/raw/deployment
# Recorrer data/ia/raw/snr

matrix = np.loadtxt(default_raw + 'users', usecols=range(100))
fig = plt.figure(frameon=False)
ax = plt.Axes(fig, [0., 0., 1., 1.])
ax.set_axis_off()
fig.add_axes(ax)
ax.imshow(matrix, cmap='Greys', aspect='equal')
fig.savefig(default_img + 'users.png')
plt.close(fig)

fig = plt.figure(frameon=False)
ax = plt.Axes(fig, [0., 0., 1., 1.])
ax.set_axis_off()
fig.add_axes(ax)
ax.imshow(matrix, cmap='Greys', aspect='equal', interpolation='hamming')
fig.savefig(default_img_inter + 'users.png')
plt.close(fig)

matrix = np.loadtxt(default_raw + 'pathloss', usecols=range(100))
fig = plt.figure(frameon=False)
ax = plt.Axes(fig, [0., 0., 1., 1.])
ax.set_axis_off()
fig.add_axes(ax)
ax.imshow(matrix, cmap='RdYlGn_r', aspect='equal', norm=colors.Normalize(2, 3))
fig.savefig(default_img + 'pathloss.png')
plt.close(fig)

fig = plt.figure(frameon=False)
ax = plt.Axes(fig, [0., 0., 1., 1.])
ax.set_axis_off()
fig.add_axes(ax)
ax.imshow(matrix, cmap='RdYlGn_r', aspect='equal', norm=colors.Normalize(2, 3), interpolation='hamming')
fig.savefig(default_img_inter + 'pathloss.png')
plt.close(fig)

for counter in range(2000, max + 1):

    print(counter)

    # SNR max
    matrix = np.loadtxt(default_raw + 'snr/{}_max'.format(counter), usecols=range(100))
    fig = plt.figure(frameon=False)
    ax = plt.Axes(fig, [0., 0., 1., 1.])
    ax.set_axis_off()
    fig.add_axes(ax)
    ax.imshow(matrix, cmap='RdYlGn', aspect='equal', norm=colors.Normalize(10, 35))
    fig.savefig(default_img + 'snr/{}_max.png'.format(counter))
    plt.close(fig)

    fig = plt.figure(frameon=False)
    ax = plt.Axes(fig, [0., 0., 1., 1.])
    ax.set_axis_off()
    fig.add_axes(ax)
    ax.imshow(matrix, cmap='RdYlGn', aspect='equal', interpolation='hamming', norm=colors.Normalize(10, 35))
    fig.savefig(default_img_inter + 'snr/{}_max.png'.format(counter))
    plt.close(fig)

    for tipo in ['micro', 'pico', 'femto']:

        # SNR
        matrix = np.loadtxt(default_raw + 'snr/{}_{}'.format(counter, tipo), usecols=range(100))
        fig = plt.figure(frameon=False)
        ax = plt.Axes(fig, [0., 0., 1., 1.])
        ax.set_axis_off()
        fig.add_axes(ax)
        ax.imshow(matrix, cmap='RdYlGn', aspect='equal', norm=colors.Normalize(10, 35))
        fig.savefig(default_img + 'snr/{}_{}.png'.format(counter, tipo))
        plt.close(fig)

        fig = plt.figure(frameon=False)
        ax = plt.Axes(fig, [0., 0., 1., 1.])
        ax.set_axis_off()
        fig.add_axes(ax)
        ax.imshow(matrix, cmap='RdYlGn', aspect='equal', interpolation='hamming', norm=colors.Normalize(10, 35))
        fig.savefig(default_img_inter + 'snr/{}_{}.png'.format(counter, tipo))
        plt.close(fig)

        # Deployment
        matrix = np.loadtxt(default_raw + '/deployment/{}_{}'.format(counter, tipo), usecols=range(100))
        fig = plt.figure(frameon=False)
        ax = plt.Axes(fig, [0., 0., 1., 1.])
        ax.set_axis_off()
        fig.add_axes(ax)
        ax.imshow(matrix, cmap='Greys', aspect='equal')
        fig.savefig(default_img + '/deployment/{}_{}.png'.format(counter, tipo))
        plt.close(fig)

        fig = plt.figure(frameon=False)
        ax = plt.Axes(fig, [0., 0., 1., 1.])
        ax.set_axis_off()
        fig.add_axes(ax)
        ax.imshow(matrix, cmap='Greys', aspect='equal', interpolation='hamming')
        fig.savefig(default_img_inter + '/deployment/{}_{}.png'.format(counter, tipo))
        plt.close(fig)
